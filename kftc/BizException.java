public class BizException extends RuntimeException {

    private final String    txId;
    private final BizStatus status;
    private final TxRecord  existingTx;

    private BizException(String txId, BizStatus status, TxRecord existingTx, String message) {
        super(message);
        this.txId       = txId;
        this.status     = status;
        this.existingTx = existingTx;
    }

    private BizException(String txId, BizStatus status, TxRecord existingTx, String message, Throwable cause) {
        super(message, cause);
        this.txId       = txId;
        this.status     = status;
        this.existingTx = existingTx;
    }

    public String    getTxId()       { return txId; }
    public BizStatus getStatus()     { return status; }
    public TxRecord  getExistingTx() { return existingTx; }

    // ── DUPLICATE_TX ───────────────────────────────────────────────────────
    public static class DuplicateTxException extends BizException {
        public DuplicateTxException(String txId, BizStatus status) {
            super(txId, status, null, "중복 전문 - txId=" + txId + ", status=" + status);
        }
    }

    // ── REPLY_ORIGINAL ─────────────────────────────────────────────────────
    public static class ReplyOriginalException extends BizException {
        public ReplyOriginalException(String txId, BizStatus status, TxRecord existingTx) {
            super(txId, status, existingTx, "재전송 대상 - txId=" + txId + ", status=" + status);
        }
    }

    // ── SAVE_REVERSE ───────────────────────────────────────────────────────
    public static class SaveReverseException extends BizException {
        public SaveReverseException(String txId) {
            super(txId, null, null, "원거래 미존재 - txId=" + txId);
        }
    }

    // ── CANCEL_NORMAL_FOR_ERROR ────────────────────────────────────────────
    public static class CancelNormalForErrorException extends BizException {
        public CancelNormalForErrorException(String txId) {
            super(txId, BizStatus.ERROR, null, "출금실패건 취소정상처리 - txId=" + txId);
        }
    }

    // ── CANCEL ─────────────────────────────────────────────────────────────
    public static class CancelException extends BizException {
        public CancelException(String message, Throwable cause) {
            super(null, null, null, message, cause);
        }
    }
}