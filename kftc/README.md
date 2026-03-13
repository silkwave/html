# KFTC 거래 상태 처리 명세

금융결제원(KFTC) 연동 시스템의 거래 유형별 상태 처리 기준을 정의합니다.

---

## 목차

1. [열거형 정의](#열거형-정의)
2. [거래 유형별 처리 기준](#거래-유형별-처리-기준)
3. [핵심 컴포넌트](#핵심-컴포넌트)
4. [처리 흐름](#처리-흐름)
5. [설계 원칙](#설계-원칙)

---

## 열거형 정의

### `BizStatus` — 거래 상태코드

| 값 | 설명 |
|---|---|
| `INIT` | 초기 수신 |
| `NORMAL` | 정상 완료 |
| `ERROR` | 오류 |
| `PROCESSING` | 처리 중 |
| `CANCEL_NORMAL` | 취소 완료 |
| `CANCEL_ERROR` | 취소 오류 |
| `CANCEL_PROC` | 취소 처리 중 |

### `ProcessResult` — 처리 결과 지시자

| 값 | 설명 |
|---|---|
| `PROCEED` | 정상 프로세스 진행 |
| `DROP` | 중복 전문 무시 |
| `REPLY_ORIGINAL` | 원거래 결과 그대로 응답 (멱등성) |
| `ERROR_NOT_FOUND` | 원거래 없음 오류 응답 |
| `SAVE_REVERSE` | 역전 거래 대비 우선 저장 |

---

## 거래 유형별 처리 기준

### 1. 조회 (`INQUIRY`)

| 원거래 존재 | 상태코드 | 처리 결과 | 비고 |
|---|---|---|---|
| 존재 | `NORMAL` / `ERROR` / `PROCESSING` | `DROP` | 중복 조회 전문 무시 |
| 미존재 | — | `PROCEED` | 조회 업무 신규 처리 |

---

### 2. 입금 (`DEPOSIT`)

| 원거래 존재 | 상태코드 | 처리 결과 | 비고 |
|---|---|---|---|
| 존재 | `NORMAL` | `REPLY_ORIGINAL` | 입금 멱등 응답 |
| 존재 | `ERROR` | `REPLY_ORIGINAL` | 입금 멱등 응답 |
| 존재 | `PROCESSING` | `DROP` | 입금 처리중 전문 무시 |
| 미존재 | — | `PROCEED` | 입금 신규 처리 |

---

### 3. 입금취소 (`DEPOSIT_CANCEL`)

| 원거래 존재 | 상태코드 | 처리 결과 | 비고 |
|---|---|---|---|
| 존재 | `NORMAL` | `PROCEED` |  입금취소 처리 진행 |
| 존재 | `CANCEL_ERROR` | `PROCEED` |  입금취소 처리 진행 |
| 존재 | `PROCESSING` | `DROP` | 입금**** 처리중 전문 무시 |
| 존재 | `CANCEL_PROC` | `DROP` | 입금취소 처리중 전문 무시 |
| 존재 | `CANCEL_NORMAL` | `REPLY_ORIGINAL` | 입금취소 멱등 응답 |
| 미존재 | — | `ERROR_NOT_FOUND` | ⚠️ 있으면 안 되는 상황 |

---

### 4. 출금 (`WITHDRAW`)

| 원거래 존재 | 상태코드 | 처리 결과 | 비고 |
|---|---|---|---|
| 존재 | — | `DROP` | 출금 전문 무시 (출금취소 유도) |
| 미존재 | — | `PROCEED` | 출금 신규 처리 |

---

### 5. 출금취소 (`WITHDRAW_CANCEL`)

| 원거래 존재 | 상태코드 | 처리 결과 | 비고 |
|---|---|---|---|
| 존재 | `NORMAL` | `PROCEED` | 출금 취소 처리 진행 |
| 존재 | `PROCESSING` | `DROP` | 출금****  처리중 전문 무시 |
| 존재 | `CANCEL_PROC` | `DROP` | 출금취소 처리중 전문 무시 |
| 존재 | `CANCEL_NORMAL` | `REPLY_ORIGINAL` | 출금취소정상 멱등 응답 |
| 존재 | `ERROR` | `REPLY_ORIGINAL` |  출금 취소정상 응답 및 출금 취소응답코드 저장 |
| 미존재 | `CANCEL_NORMAL` | `SAVE_REVERSE` | ⚠️ 출금 취소정상 응답 및 출금 취소 전문 저장 |

---

## 핵심 컴포넌트

### `KftcStatusEngine`

`(bizType, BizStatus)` 조합으로 `ProcessResult`를 결정하는 상태 머신입니다.

```java
@Component
public class KftcStatusEngine {

    public ProcessResult determineAction(String bizType, BizStatus currentStatus) {
        return switch (bizType) {
            case "INQUIRY"         -> (currentStatus != null) ? ProcessResult.DROP           : ProcessResult.PROCEED;
            case "DEPOSIT"         -> (currentStatus != null) ? ProcessResult.REPLY_ORIGINAL : ProcessResult.PROCEED;
            case "DEPOSIT_CANCEL"  -> handleDepositCancel(currentStatus);
            case "WITHDRAW_CANCEL" -> handleWithdrawCancel(currentStatus);
            default                -> ProcessResult.PROCEED;
        };
    }

    private ProcessResult handleDepositCancel(BizStatus status) {
        if (status == null) return ProcessResult.ERROR_NOT_FOUND;
        return switch (status) {
            case NORMAL                    -> ProcessResult.PROCEED;
            case CANCEL_ERROR, CANCEL_PROC -> ProcessResult.DROP;
            case CANCEL_NORMAL, PROCESSING -> ProcessResult.REPLY_ORIGINAL;
            default                        -> ProcessResult.DROP;
        };
    }
}
```

### `KftcTransactionService`

`KftcStatusEngine`이 결정한 `ProcessResult`에 따라 실제 처리를 분기합니다.

```java
@Service
@Transactional
public ResponseDto processTransaction(RequestDto request) {

    // 1. SELECT FOR UPDATE — 원거래 락 획득 (중복 처리 방지)
    TransactionEntity originTx = repository.findByTrxIdWithLock(request.getTrxId());

    // 2. 상태 판단
    BizStatus currentStatus = (originTx != null) ? originTx.getStatus() : null;
    ProcessResult action = statusEngine.determineAction(request.getBizType(), currentStatus);

    // 3. 결과별 분기
    return switch (action) {
        case PROCEED         -> executeBusinessLogic(request);        // 업무 로직 실행
        case DROP            -> ResponseDto.ignore();                  // 무응답 종료
        case REPLY_ORIGINAL  -> ResponseDto.fromEntity(originTx);     // 기존 결과 재전송
        case ERROR_NOT_FOUND -> ResponseDto.error("404", "원거래 없음");
        case SAVE_REVERSE    -> saveReverseTransaction(request);      // 역전 대비 저장
    };
}
```

---

## 처리 흐름

```
전문 수신
    │
    ├─ SELECT FOR UPDATE (원거래 조회 + 행 레벨 락)
    │
    ├─ KftcStatusEngine.determineAction(bizType, currentStatus)
    │       │
    │       ├─ PROCEED         ──► 업무 로직 실행 → 원장 반영
    │       ├─ DROP            ──► 무시, 종료
    │       ├─ REPLY_ORIGINAL  ──► 원거래 응답 재전송 (멱등성 보장)
    │       ├─ ERROR_NOT_FOUND ──► 404 오류 응답
    │       └─ SAVE_REVERSE    ──► 역전 거래 선행 저장
    │
    └─ ResponseDto 반환
```

---

## 설계 원칙

**멱등성 (Idempotency)**
동일 전문이 재수신되면 원거래 응답을 그대로 반환합니다 (`REPLY_ORIGINAL`). 네트워크 재전송이나 타임아웃 재시도 상황에서 중복 처리를 방지합니다.

**역전 거래 방어 (Reverse Transaction Guard)**
출금취소 전문이 출금 원거래보다 먼저 도착한 경우, 취소 내역을 선행 저장합니다 (`SAVE_REVERSE`). 이후 출금 원거래가 도착했을 때 선저장된 취소 내역을 확인하여 정합성을 보장합니다.

**중복 전문 무시 (Duplicate Drop)**
이미 처리 중이거나 완료된 거래에 대한 재요청은 `DROP`으로 무시합니다. 취소 처리 중(`CANCEL_PROC`) 상태에서 동일 취소 전문이 재수신되는 경우를 포함합니다.

**비관적 락 (Pessimistic Locking)**
`SELECT FOR UPDATE`로 원거래 레코드에 행 레벨 락을 획득한 뒤 상태를 판단합니다. 동시 요청으로 인한 경쟁 조건(Race Condition)을 원천 차단합니다.
