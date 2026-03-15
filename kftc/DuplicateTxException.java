public class DuplicateTxException extends RuntimeException {

    private final BizStatus status;

    public DuplicateTxException(String txId, BizStatus status) {
        super("중복 전문 - txId=" + txId + ", status=" + status);
        this.status = status;
    }

    public BizStatus getStatus() {
        return status;
    }
}