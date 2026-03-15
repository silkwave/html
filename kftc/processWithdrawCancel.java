public void processWithdrawCancel(WithdrawCancelRequest request) {
    String txId = request.getTxId();

    try {
        // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────
        TxRecord existingTx = txRepository.findByTxIdForUpdate(txId);

        // ── 2. 원거래 미존재 → SAVE_REVERSE 저장 후 정상 응답 반환 ─────
        if (existingTx == null) {
            log.warn("[WITHDRAW_CANCEL] SAVE_REVERSE - txId={} (원거래 미존재)", txId);
            txRepository.save(TxRecord.builder()
                    .txId(txId)
                    .txType(TxType.WITHDRAW_CANCEL)
                    .status(BizStatus.SAVE_REVERSE)
                    .isReverse(true)
                    .build());
            sendNormalResponse(request);
            return;
        }

        // ── 3. 원거래 존재 → 상태 분기 ────────────────────────────────
        BizStatus status = existingTx.getStatus();

        // 진행 중 → DROP
        if (isInProgress(status)) {
            throw new DuplicateTxException(txId, status);
        }

        // 취소 완료 → 기존 응답 재전송
        if (status == BizStatus.CANCEL_NORMAL) {
            throw new ReplyOriginalException(txId, status, existingTx);
        }

        // 출금 자체 실패 → 취소할 금액 없음, 취소정상 처리
        if (status == BizStatus.ERROR) {
            txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, "출금실패건_취소정상처리");
            log.info("[WITHDRAW_CANCEL] CANCEL_NORMAL (출금 ERROR 건) - txId={}", txId);
            sendNormalResponse(request);
            return;
        }

        // ── 4. NORMAL / CANCEL_ERROR → CANCEL_PROC 선기록 후 취소 실행
        txRepository.updateStatus(txId, BizStatus.CANCEL_PROC, null);
        withdrawCancelService.execute(request, existingTx);

        txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, null);
        log.info("[WITHDRAW_CANCEL] CANCEL_NORMAL - txId={}", txId);
        sendNormalResponse(request);

    } catch (Exception e) {

        // 중복 전문 → DROP
        if (e instanceof DuplicateTxException dupEx) {
            log.info("[WITHDRAW_CANCEL] DROP - txId={}, status={}", txId, dupEx.getStatus());
            return;
        }

        // 취소 완료 → 기존 응답 재전송
        if (e instanceof ReplyOriginalException replyEx) {
            log.info("[WITHDRAW_CANCEL] REPLY_ORIGINAL - txId={}", txId);
            sendOriginalResponse(request, replyEx.getExistingTx());
            return;
        }

        // 취소 실패 → CANCEL_ERROR 저장 및 오류 전문 전송
        log.warn("[WITHDRAW_CANCEL] CANCEL_ERROR - txId={}, reason={}", txId, e.getMessage());
        txRepository.updateStatus(txId, BizStatus.CANCEL_ERROR, e.getMessage());
        sendErrorResponse(request, e);
    }
}

private boolean isInProgress(BizStatus status) {
    return status == BizStatus.CANCEL_PROC
        || status == BizStatus.PROCESSING;
}

private boolean isProceedable(BizStatus status) {
    return status == BizStatus.NORMAL
        || status == BizStatus.CANCEL_ERROR;
}