public DepositCancelResult processDepositCancel(DepositCancelRequest request) {
    String txId = request.getTxId();

    // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────────────
    TxRecord existing = txRepository.findByTxIdForUpdate(txId);

    // ── 2. 원거래 존재 분기 ────────────────────────────────────────────────
    if (existing != null) {
        BizStatus status = existing.getStatus();

        // 진행 중 → DROP
        if (status == BizStatus.CANCEL_PROC || status == BizStatus.PROCESSING) {
            log.info("[DEPOSIT_CANCEL] DROP - txId={}, status={}", txId, status);
            return DepositCancelResult.dropped();
        }

        // 취소 완료 → 기존 응답 재전송
        if (status == BizStatus.CANCEL_NORMAL) {
            log.info("[DEPOSIT_CANCEL] REPLY_ORIGINAL - txId={}", txId);
            return DepositCancelResult.replyOriginal(existing);
        }

        // NORMAL / CANCEL_ERROR → 재취소 시도 (PROCEED)
        if (status == BizStatus.NORMAL || status == BizStatus.CANCEL_ERROR) {
            log.info("[DEPOSIT_CANCEL] PROCEED - txId={}, status={}", txId, status);
            return executeCancel(txId, request, existing);
        }
    }

    // ── 3. 원거래 미존재 → REVERSE 저장 후 정상 응답 반환 ─────────────────
    // 정상 흐름에서 발생 불가 - 역거래 방어 처리
    // (입금취소 선수신 → 이후 입금 수신 시 REVERSE 레코드로 차단)
    log.warn("[DEPOSIT_CANCEL] REVERSE - txId={} (원거래 미존재)", txId);

    txRepository.save(TxRecord.builder()
            .txId(txId)
            .txType(TxType.DEPOSIT_CANCEL)
            .status(BizStatus.CANCEL_NORMAL)
            .isReverse(true)
            .build());

    return DepositCancelResult.reverseOk();
}

// ── 취소 실행 (CANCEL_PROC 선기록 → 자금 회수 → 상태 확정) ──────────────────
private DepositCancelResult executeCancel(String txId,
                                          DepositCancelRequest request,
                                          TxRecord existing) {
    // CANCEL_PROC 선기록 (방어막 형성)
    txRepository.updateStatus(txId, BizStatus.CANCEL_PROC, null);

    try {
        DepositCancelResult result = depositCancelService.execute(request, existing);

        txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, null);
        log.info("[DEPOSIT_CANCEL] CANCEL_NORMAL - txId={}", txId);

        return result;

    } catch (CancelTimeoutException | CancelSystemException e) {
        txRepository.updateStatus(txId, BizStatus.CANCEL_ERROR, e.getMessage());
        log.warn("[DEPOSIT_CANCEL] CANCEL_ERROR - txId={}, reason={}", txId, e.getMessage());

        return DepositCancelResult.error(e.getMessage());
    }
}