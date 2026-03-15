public class ReplyOriginalException extends RuntimeException {

    private final BizStatus status;
    private final TxRecord existingTx;

    public ReplyOriginalException(String txId, BizStatus status, TxRecord existingTx) {
        super("재전송 대상 - txId=" + txId + ", status=" + status);
        this.status = status;
        this.existingTx = existingTx;
    }

    public BizStatus getStatus() { return status; }
    public TxRecord getExistingTx() { return existingTx; }
}