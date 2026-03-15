public WithdrawResult processWithdraw(WithdrawRequest request) {
    String txId = request.getTxId();

    // ── 1. 원거래 조회 (SELECT FOR UPDATE NOWAIT) ──────────────────────────
    TxRecord existing = txRepository.findByTxIdForUpdate(txId);

    // ── 2. 원거래 존재 → 상태 무관 무조건 DROP ────────────────────────────
    // 출금은 금전적 리스크 최우선 — 정정 필요 시 출금취소 후 재출금
    if (existing != null) {
        log.warn("[WITHDRAW] DROP - txId={}, status={}", txId, existing.getStatus());
        return WithdrawResult.dropped();
    }

    // ── 3. 신규 요청 → PROCESSING 선기록 (중복 출금 방어막) ───────────────
    txRepository.save(TxRecord.builder()
            .txId(txId)
            .txType(TxType.WITHDRAW)
            .status(BizStatus.PROCESSING)
            .build());

    // ── 4. 출금 실행 ───────────────────────────────────────────────────────
    try {
        WithdrawResult result = withdrawService.execute(request);

        txRepository.updateStatus(txId, BizStatus.NORMAL, null);
        log.info("[WITHDRAW] NORMAL - txId={}", txId);

        return result;

    } catch (LimitExceededException | InsufficientBalanceException e) {
        txRepository.updateStatus(txId, BizStatus.ERROR, e.getMessage());
        log.warn("[WITHDRAW] ERROR - txId={}, reason={}", txId, e.getMessage());

        return WithdrawResult.error(e.getMessage());
    }
}