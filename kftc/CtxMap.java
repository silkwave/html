import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 메서드 체이닝을 지원하는 컨텍스트 Map 래퍼.
 * <p>
 * 사용 예:
 * <pre>
 *   CtxMap ctx = CtxMap.of("txId", txId, "request", request);
 *
 *   String txId = ctx.require("txId");
 *   String val  = ctx.getOrDefault("optional", "default");
 * </pre>
 */
public class CtxMap {

    private final Map<String, Object> map;

    // ────────────────────────────────────────────
    // 생성자
    // ────────────────────────────────────────────

    public CtxMap() {
        this.map = new LinkedHashMap<>();
    }

    public CtxMap(Map<String, Object> initial) {
        this.map = new LinkedHashMap<>(initial);
    }

    // ────────────────────────────────────────────
    // 정적 팩토리
    // ────────────────────────────────────────────

    public static CtxMap of(String k1, Object v1) {
        return new CtxMap().put(k1, v1);
    }

    public static CtxMap of(String k1, Object v1, String k2, Object v2) {
        return new CtxMap().put(k1, v1).put(k2, v2);
    }

    public static CtxMap of(String k1, Object v1, String k2, Object v2,
                            String k3, Object v3) {
        return new CtxMap().put(k1, v1).put(k2, v2).put(k3, v3);
    }

    public static CtxMap of(String k1, Object v1, String k2, Object v2,
                            String k3, Object v3, String k4, Object v4) {
        return new CtxMap().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4);
    }

    // ────────────────────────────────────────────
    // 쓰기
    // ────────────────────────────────────────────

    /** 값 저장 후 this 반환 (체이닝용) */
    public CtxMap put(String key, Object value) {
        map.put(key, value);
        return this;
    }

    /** 다른 CtxMap 병합 */
    public CtxMap merge(CtxMap other) {
        map.putAll(other.map);
        return this;
    }

    /** 키 제거 후 this 반환 (체이닝용) */
    public CtxMap remove(String key) {
        map.remove(key);
        return this;
    }

    // ────────────────────────────────────────────
    // 읽기
    // ────────────────────────────────────────────

    /** 타입 캐스팅 포함 조회. 키 없으면 null 반환 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) map.get(key);
    }

    /** 기본값 포함 조회 */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) map.getOrDefault(key, defaultValue);
    }

    /**
     * 필수값 조회. 키가 없으면 예외 발생.
     * 금융 트랜잭션 등 필수 컨텍스트 키 누락을 조기에 감지할 때 사용.
     */
    @SuppressWarnings("unchecked")
    public <T> T require(String key) {
        if (!map.containsKey(key)) {
            throw new IllegalStateException("CtxMap missing required key: " + key);
        }
        return (T) map.get(key);
    }

    // ────────────────────────────────────────────
    // 상태 조회
    // ────────────────────────────────────────────

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    /** 내부 Map 불변 복사본 반환 */
    public Map<String, Object> toMap() {
        return Map.copyOf(map);
    }

    // ────────────────────────────────────────────
    // Object
    // ────────────────────────────────────────────

    @Override
    public String toString() {
        if (map.isEmpty()) return "CtxMap{}";
        StringBuilder sb = new StringBuilder("CtxMap{");
        map.forEach((k, v) -> sb.append(k).append("=").append(v).append(", "));
        sb.setLength(sb.length() - 2);
        return sb.append("}").toString();
    }
}