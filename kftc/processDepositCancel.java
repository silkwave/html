public void processDepositCancel(DepositCancelRequest request) {
    String txId = request.getTxId();

    CtxMap ctx = new CtxMap();
    ctx.put("txId",    txId)
       .put("request", request);

    try {
        // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────
        TxRecord existingTx = txRepository.findByTxIdForUpdate(txId);

        // ── 2. 원거래 미존재 → REVERSE 예외 throw ──────────────────────
        if (existingTx == null) {
            throw new SaveReverseException(txId);
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

        // ── 4. NORMAL / CANCEL_ERROR → CANCEL_PROC 선기록 후 취소 실행
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
    String               txId    = ctx.getString("txId");
    DepositCancelRequest request = ctx.getObject("request",   DepositCancelRequest.class);
    Exception            e       = ctx.getObject("exception", Exception.class);

    // 원거래 미존재 → REVERSE 저장 및 정상 응답 반환
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

    // 중복 전문 → DROP
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