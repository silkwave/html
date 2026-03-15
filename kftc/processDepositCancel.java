public void processDepositCancel(DepositCancelRequest request) {
    String txId = request.getTxId();

    CtxMap ctx = new CtxMap();
    ctx.put("txId",    txId)
       .put("request", request);

    try {
        // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────
        TxRecord existingTx = txRepository.findByTxIdForUpdate(txId);

        // ── 2. 원거래 미존재 → REVERSE ─────────────────────────────────
        if (existingTx == null) {
            throw new SaveReverseException(txId);
        }

        // ── 3. 원거래 존재 → 상태 분기 ────────────────────────────────
        BizStatus status = existingTx.getStatus();

        if (isInProgress(status))              throw new DuplicateTxException(txId, status);               // 진행 중 → DROP
        if (status == BizStatus.CANCEL_NORMAL) throw new ReplyOriginalException(txId, status, existingTx); // 취소 완료 → 재전송
        if (status == BizStatus.ERROR)         throw new CancelNormalForErrorException(txId);              // 입금 실패 → 취소정상

        // ── 4. NORMAL / CANCEL_ERROR → CANCEL_PROC 선기록 후 취소 실행 ─
        txRepository.updateStatus(txId, BizStatus.CANCEL_PROC, null);
        depositCancelService.execute(request, existingTx);

        txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, null);
        log.info("[DEPOSIT_CANCEL] CANCEL_NORMAL - txId={}", txId);
        sendNormalResponse(request);

    } catch (Exception e) {
        ctx.put("exception", e);
        handleDepositCancelException(ctx);
    }
}

private void handleDepositCancelException(CtxMap ctx) {
    String               txId    = ctx.require("txId");
    DepositCancelRequest request = ctx.require("request");
    Exception            e       = ctx.require("exception");

    // 원거래 미존재 → SAVE_REVERSE 저장 및 정상 응답
    if (e instanceof SaveReverseException) {
        log.warn("[DEPOSIT_CANCEL] REVERSE - txId={} (원거래 미존재)", txId);
        txRepository.save(TxRecord.builder()
                .txId(txId)
                .txType(TxType.DEPOSIT_CANCEL)
                .status(BizStatus.CANCEL_NORMAL)
                .isReverse(true)
                .build());
        sendNormalResponse(request);
        return;
    }

    // 진행 중 중복 전문 → DROP
    if (e instanceof DuplicateTxException dupEx) {
        log.info("[DEPOSIT_CANCEL] DROP - txId={}, status={}", txId, dupEx.getStatus());
        return;
    }

    // 취소 완료 → 기존 응답 재전송
    if (e instanceof ReplyOriginalException replyEx) {
        log.info("[DEPOSIT_CANCEL] REPLY_ORIGINAL - txId={}", txId);
        sendOriginalResponse(request, replyEx.getExistingTx());
        return;
    }

    // 입금 실패 건 → 취소정상 처리 (취소할 금액 없음)
    if (e instanceof CancelNormalForErrorException) {
        log.info("[DEPOSIT_CANCEL] CANCEL_NORMAL (입금 ERROR 건) - txId={}", txId);
        txRepository.updateStatus(txId, BizStatus.CANCEL_NORMAL, "입금실패건_취소정상처리");
        sendNormalResponse(request);
        return;
    }

    // 취소 실패 → CANCEL_ERROR 저장 및 오류 전문 전송
    log.warn("[DEPOSIT_CANCEL] CANCEL_ERROR - txId={}, reason={}", txId, e.getMessage());
    txRepository.updateStatus(txId, BizStatus.CANCEL_ERROR, e.getMessage());
    sendErrorResponse(request, e);
}

private boolean isInProgress(BizStatus status) {
    return status == BizStatus.CANCEL_PROC
        || status == BizStatus.PROCESSING;
}

private boolean isProceedable(BizStatus status) {
    return status == BizStatus.NORMAL
        || status == BizStatus.CANCEL_ERROR;
}