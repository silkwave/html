// ================================================================
// CtxMap 사용 예제 — 금융 트랜잭션 처리 컨텍스트
// ================================================================

public class CtxMapUsageExample {

    // ────────────────────────────────────────────
    // 1. 생성 패턴
    // ────────────────────────────────────────────

    void creation() {

        // (1) 기본 생성 + 체이닝
        CtxMap ctx = new CtxMap();
        ctx.put("txId",    "TX20260315001")
           .put("amount",  50000L)
           .put("request", new TransferRequest());

        // (2) 정적 팩토리 — 간결한 초기화
        CtxMap ctx2 = CtxMap.of("txId",    "TX20260315001",
                                 "amount",  50000L);

        // (3) 기존 Map으로 생성
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("txId", "TX20260315001");
        CtxMap ctx3 = new CtxMap(raw);
    }

    // ────────────────────────────────────────────
    // 2. 값 조회 패턴
    // ────────────────────────────────────────────

    void read() {

        CtxMap ctx = CtxMap.of("txId",   "TX20260315001",
                                "amount", 50000L,
                                "retry",  2);

        // (1) get() — null 허용, 선택적 값
        String memo = ctx.get("memo");          // null (키 없음)
        Long   amt  = ctx.get("amount");        // 50000L

        // (2) getOrDefault() — 선택적 값 + 기본값
        int retryCount = ctx.getOrDefault("retry",   0);   // 2
        int timeout    = ctx.getOrDefault("timeout", 30);  // 30 (기본값)

        // (3) require() — 필수값, 없으면 즉시 예외
        String txId = ctx.require("txId");      // "TX20260315001"
        String bank = ctx.require("bankCode");  // IllegalStateException!
        //  → CtxMap missing required key: bankCode
    }

    // ────────────────────────────────────────────
    // 3. 실무 패턴 — 서비스 레이어 간 컨텍스트 전달
    // ────────────────────────────────────────────

    // Controller → Service → Handler 흐름
    void serviceLayerFlow() {

        // Controller: 초기 컨텍스트 구성
        CtxMap ctx = CtxMap.of("txId",      "TX20260315001",
                                "request",   new TransferRequest(),
                                "requestAt", LocalDateTime.now());

        // Service: 중간에 값 추가
        ctx.put("userId",   "user_042")
           .put("bankCode", "088");

        // Handler: 필수값만 꺼내서 사용
        String txId    = ctx.require("txId");      // 없으면 즉시 실패
        String bank    = ctx.require("bankCode");
        int    timeout = ctx.getOrDefault("timeout", 30);

        // Handler 처리 결과를 다시 ctx에 적재
        ctx.put("resultCode", "0000")
           .put("resultMsg",  "정상처리");
    }

    // ────────────────────────────────────────────
    // 4. 병합 패턴 — 두 컨텍스트 합치기
    // ────────────────────────────────────────────

    void merge() {

        CtxMap baseCtx = CtxMap.of("txId",   "TX20260315001",
                                    "userId", "user_042");

        CtxMap extraCtx = CtxMap.of("bankCode", "088",
                                     "amount",  50000L);

        // baseCtx에 extraCtx 병합 (체이닝 유지)
        baseCtx.merge(extraCtx)
               .put("mergedAt", LocalDateTime.now());

        // 결과: txId, userId, bankCode, amount, mergedAt 모두 포함
        System.out.println(baseCtx);
        // CtxMap{txId=TX20260315001, userId=user_042, bankCode=088, amount=50000, mergedAt=...}
    }

    // ────────────────────────────────────────────
    // 5. remove + containsKey 패턴
    // ────────────────────────────────────────────

    void removeAndCheck() {

        CtxMap ctx = CtxMap.of("txId",     "TX20260315001",
                                "tempFlag", true,
                                "amount",   50000L);

        // 처리 후 임시 플래그 제거 (체이닝)
        ctx.remove("tempFlag")
           .put("processedAt", LocalDateTime.now());

        // 키 존재 여부 확인
        if (ctx.containsKey("amount")) {
            Long amt = ctx.require("amount");
            // 금액 후처리 로직
        }
    }

    // ────────────────────────────────────────────
    // 6. toMap() — MyBatis 파라미터로 넘길 때
    // ────────────────────────────────────────────

    void mybatisUsage(SqlSession sqlSession) {

        CtxMap ctx = CtxMap.of("txId",    "TX20260315001",
                                "userId",  "user_042",
                                "amount",  50000L,
                                "status",  "COMPLETED");

        // MyBatis mapper에 Map으로 전달
        Map<String, Object> params = ctx.toMap(); // 불변 복사본
        sqlSession.selectOne("TxMapper.selectByCtx", params);
    }

    // ────────────────────────────────────────────
    // 7. 로깅 패턴
    // ────────────────────────────────────────────

    void logging() {

        CtxMap ctx = CtxMap.of("txId",      "TX20260315001",
                                "bankCode",  "088",
                                "amount",    50000L,
                                "resultCode","0000");

        // toString() 자동 호출
        log.info("[TX완료] {}", ctx);
        // [TX완료] CtxMap{txId=TX20260315001, bankCode=088, amount=50000, resultCode=0000}

        // 상태 체크
        log.debug("ctx size={}, isEmpty={}", ctx.size(), ctx.isEmpty());
    }
}