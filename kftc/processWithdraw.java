public void processWithdraw(WithdrawRequest request) {
    String txId = request.getTxId();

    try {
        // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────
        TxRecord existingTx = txRepository.findByTxIdForUpdate(txId);

        // ── 2. 원거래 존재 → 상태 무관 무조건 DROP ─────────────────────
        // 출금은 금전적 리스크 최우선 — 정정 필요 시 출금취소 후 재출금
        if (existingTx != null) {
            throw new DuplicateTxException(txId, existingTx.getStatus());
        }

        // ── 3. 신규 요청 → PROCESSING 선기록 (중복 출금 방어막) ────────
        txRepository.save(TxRecord.builder()
                .txId(txId)
                .txType(TxType.WITHDRAW)
                .status(BizStatus.PROCESSING)
                .build());

        // ── 4. 출금 실행 ────────────────────────────────────────────────
        withdrawService.execute(request);

        txRepository.updateStatus(txId, BizStatus.NORMAL, null);
        log.info("[WITHDRAW] NORMAL - txId={}", txId);
        sendNormalResponse(request);

    } catch (Exception e) {

        // 원거래 존재 → 상태 무관 DROP
        if (e instanceof DuplicateTxException dupEx) {
            log.warn("[WITHDRAW] DROP - txId={}, status={}", txId, dupEx.getStatus());
            return;
        }

        // 출금 실패 → ERROR 저장 및 오류 전문 전송
        log.warn("[WITHDRAW] ERROR - txId={}, reason={}", txId, e.getMessage());
        txRepository.updateStatus(txId, BizStatus.ERROR, e.getMessage());
        sendErrorResponse(request, e);
    }
}