public DepositResult processDeposit(DepositRequest request) {
    String txId = request.getTxId();

    // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────────────
    TxRecord existing = txRepository.findByTxIdForUpdate(txId);

    if (existing != null) {
        BizStatus status = existing.getStatus();

        // 최종 확정 상태 → 기존 응답 재전송
        if (status == BizStatus.NORMAL || status == BizStatus.ERROR) {
            log.info("[DEPOSIT] REPLY_ORIGINAL - txId={}, status={}", txId, status);
            return DepositResult.replyOriginal(existing);
        }

        // 처리 중 → DROP
        if (status == BizStatus.PROCESSING) {
            log.info("[DEPOSIT] DROP - txId={}, status=PROCESSING", txId);
            return DepositResult.dropped();
        }
    }

    // ── 2. 신규 요청 → PROCESSING 선기록 (방어막 형성) ────────────────────
    TxRecord record = TxRecord.builder()
            .txId(txId)
            .status(BizStatus.PROCESSING)
            .build();
    txRepository.save(record);

    // ── 3. 입금 실행 ───────────────────────────────────────────────────────
    try {
        DepositResult result = depositService.execute(request);

        txRepository.updateStatus(txId, BizStatus.NORMAL, null);
        log.info("[DEPOSIT] NORMAL - txId={}", txId);

        return result;

    } catch (InsufficientBalanceException | AccountStatusException e) {
        txRepository.updateStatus(txId, BizStatus.ERROR, e.getMessage());
        log.warn("[DEPOSIT] ERROR - txId={}, reason={}", txId, e.getMessage());

        return DepositResult.error(e.getMessage());
    }
}