public void processInquiry(InquiryRequest request) {
    String txId = request.getTxId();

    CtxMap ctx = new CtxMap();
    ctx.put("txId",    txId)
       .put("request", request);

    try {
        // ── 1. 원거래 조회 ─────────────────────────────────────────────
        TxRecord existingTx = txRepository.findByTxId(txId);

        // ── 2. 원거래 존재 → DROP ──────────────────────────────────────
        if (existingTx != null) {
            BizStatus status = existingTx.getStatus();
            if (isDuplicate(status)) {
                throw new DuplicateTxException(txId, status);
            }
        }

        // ── 3. 원거래 미존재 → 조회 실행 ──────────────────────────────
        inquiryService.execute(request);

        // ── 4. 조회 성공 → NORMAL 저장 및 정상 전문 전송 ──────────────
        txRepository.save(TxRecord.builder()
                .txId(txId)
                .txType(TxType.INQUIRY)
                .status(BizStatus.NORMAL)
                .build());

        log.info("[INQUIRY] NORMAL - txId={}", txId);
        sendNormalResponse(request);

    } catch (Exception e) {
        ctx.put("exception", e);
        handleInquiryException(ctx);
    }
}

private void handleInquiryException(CtxMap ctx) {
    String         txId    = ctx.require("txId");
    InquiryRequest request = ctx.require("request");
    Exception      e       = ctx.require("exception");

    // 중복 전문 → DROP 처리
    if (e instanceof DuplicateTxException dupEx) {
        log.info("[INQUIRY] DROP - txId={}, status={}", txId, dupEx.getStatus());
        return;
    }

    // 모든 예외 → ERROR 저장 및 오류 전문 전송
    log.error("[INQUIRY] 예외 발생 - txId={}, reason={}", txId, e.getMessage(), e);

    txRepository.save(TxRecord.builder()
            .txId(txId)
            .txType(TxType.INQUIRY)
            .status(BizStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());

    sendErrorResponse(request, e);
}

private boolean isDuplicate(BizStatus status) {
    return status == BizStatus.NORMAL
        || status == BizStatus.ERROR
        || status == BizStatus.PROCESSING;
}