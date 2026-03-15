public void processDeposit(DepositRequest request) {
    String txId = request.getTxId();

    try {
        // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────
        TxRecord existingTx = txRepository.findByTxIdForUpdate(txId);

        // ── 2. 원거래 존재 → 상태 분기 ────────────────────────────────
        if (existingTx != null) {
            BizStatus status = existingTx.getStatus();

            // 최종 확정 상태 → 기존 응답 재전송
            if (isFinalStatus(status)) {
                throw new ReplyOriginalException(txId, status, existingTx);
            }

            // 처리 중 → DROP
            if (status == BizStatus.PROCESSING) {
                throw new DuplicateTxException(txId, status);
            }
        }

        // ── 3. 신규 요청 → PROCESSING 선기록 (방어막 형성) ────────────
        txRepository.save(TxRecord.builder()
                .txId(txId)
                .txType(TxType.DEPOSIT)
                .status(BizStatus.PROCESSING)
                .build());

        // ── 4. 입금 실행 → NORMAL 저장 및 정상 전문 전송 ──────────────
        depositService.execute(request);

        txRepository.updateStatus(txId, BizStatus.NORMAL, null);
        log.info("[DEPOSIT] NORMAL - txId={}", txId);
        sendNormalResponse(request);

    } catch (Exception e) {

        // 최종 확정 상태 → 기존 응답 재전송
        if (e instanceof ReplyOriginalException replyEx) {
            log.info("[DEPOSIT] REPLY_ORIGINAL - txId={}, status={}", txId, replyEx.getStatus());
            sendOriginalResponse(request, replyEx.getExistingTx());
            return;
        }

        // 중복 전문 → DROP 처리
        if (e instanceof DuplicateTxException dupEx) {
            log.info("[DEPOSIT] DROP - txId={}, status={}", txId, dupEx.getStatus());
            return;
        }

        // 모든 예외 → ERROR 저장 및 오류 전문 전송
        log.error("[DEPOSIT] 예외 발생 - txId={}, reason={}", txId, e.getMessage(), e);

        txRepository.updateStatus(txId, BizStatus.ERROR, e.getMessage());
        sendErrorResponse(request, e);
    }
}

private boolean isFinalStatus(BizStatus status) {
    return status == BizStatus.NORMAL
        || status == BizStatus.ERROR;
}