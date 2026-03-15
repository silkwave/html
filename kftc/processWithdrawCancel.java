public WithdrawCancelResult processWithdrawCancel(WithdrawCancelRequest request) {
    String txId = request.getTxId();

    // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────────────
    TxRecord existing = txRepository.findByTxIdForUpdate(txId);

    // ── 2. 원거래 존재 분기 ────────────────────────────────────────────────
    if (existing != null) {
        BizStatus status = existing.getStatus();

        // 진행 중 → DROP
        if (status == BizStatus.CANCEL_PROC || status == BizStatus.PROCESSING) {
            log.info("[WITHDRAW_CANCEL] DROP - txId={}, status={}", txId, status);
            return WithdrawCancelResult.dropped();
        }

        // 취소 완료 → 기존 응답 재전송
        if (status == BizStatus.CANCEL_NORMAL) {
            log.info("[WITHDRAW_CANCEL] REPLY_ORIGINAL - txId={}", txId);
            return WithdrawCancelResult.replyOriginal(existing);
        }

        // 출금 자체 실패 → 취소할 금액 없음, 취소정상 응답 후 종료
        if (status == BizStatus.ERROR) {
            log.info("[WITHDRAW_CANCEL] CANCEL_NORMAL (출금 ERROR 건) - txId={}", txId);
            txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, "출금실패건_취소정상처리");
            return WithdrawCancelResult.okNoAction();
        }

        // NORMAL / CANCEL_ERROR → 취소 실행 (PROCEED)
        if (status == BizStatus.NORMAL || status == BizStatus.CANCEL_ERROR) {
            log.info("[WITHDRAW_CANCEL] PROCEED - txId={}, status={}", txId, status);
            return executeCancel(txId, request, existing);
        }
    }

    // ── 3. 원거래 미존재 → REVERSE 저장 후 정상 응답 반환 ─────────────────
    // 정상 흐름에서 발생 불가 - 역거래 방어 처리
    // (출금취소 선수신 → 이후 출금 수신 시 REVERSE 레코드로 차단)
    log.warn("[WITHDRAW_CANCEL] SAVE_REVERSE - txId={} (원거래 미존재)", txId);

    txRepository.save(TxRecord.builder()
            .txId(txId)
            .txType(TxType.WITHDRAW_CANCEL)
            .status(BizStatus.SAVE_REVERSE)
            .isReverse(true)
            .build());

    return WithdrawCancelResult.reverseOk();
}

// ── 취소 실행 (CANCEL_PROC 선기록 → 잔액 복구 → 상태 확정) ──────────────────
private WithdrawCancelResult executeCancel(String txId,
                                           WithdrawCancelRequest request,
                                           TxRecord existing) {
    // CANCEL_PROC 선기록 (방어막 형성)
    txRepository.updateStatus(txId, BizStatus.CANCEL_PROC, null);

    try {
        WithdrawCancelResult result = withdrawCancelService.execute(request, existing);

        txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, null);
        log.info("[WITHDRAW_CANCEL] CANCEL_NORMAL - txId={}", txId);

        return result;

    } catch (CancelTimeoutException | CancelSystemException e) {
        txRepository.updateStatus(txId, BizStatus.CANCEL_ERROR, e.getMessage());
        log.warn("[WITHDRAW_CANCEL] CANCEL_ERROR - txId={}, reason={}", txId, e.getMessage());

        return WithdrawCancelResult.error(e.getMessage());
    }
}