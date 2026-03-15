public InquiryResult processInquiry(InquiryRequest request) {
    String txId = request.getTxId();

    // 원거래 조회
    TxRecord existing = txRepository.findByTxId(txId);

    // 원거래 존재 + 처리됐거나 처리 중 → DROP
    if (existing != null) {
        BizStatus status = existing.getStatus();
        if (status == BizStatus.NORMAL
                || status == BizStatus.ERROR
                || status == BizStatus.PROCESSING) {
            log.info("[INQUIRY] DROP - txId={}, status={}", txId, status);
            return InquiryResult.dropped();
        }
    }

    // 원거래 미존재 → 최초 요청, 정상 처리
    try {
        InquiryResult result = inquiryService.execute(request);

        txRepository.save(TxRecord.builder()
                .txId(txId)
                .status(BizStatus.NORMAL)
                .build());

        return result;

    } catch (AccountNotFoundException | ExternalAgencyException e) {
        log.warn("[INQUIRY] ERROR - txId={}, reason={}", txId, e.getMessage());

        txRepository.save(TxRecord.builder()
                .txId(txId)
                .status(BizStatus.ERROR)
                .errorMessage(e.getMessage())
                .build());

        return InquiryResult.error(e.getMessage());
    }
}