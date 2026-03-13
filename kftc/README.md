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
| --- | --- |
| `INIT` | 거래 전문이 최초로 시스템에 수신된 상태. 전문 파싱 및 기본 검증 단계 |
| `NORMAL` | 거래가 정상적으로 완료된 상태. 비즈니스 로직 성공 |
| `ERROR` | 거래 처리 중 오류가 발생하여 실패한 상태. 계좌 검증 실패, 잔액 부족 등 |
| `PROCESSING` | 거래가 현재 처리 중인 상태. 외부 기관 응답 대기 또는 로직 실행 중 |
| `CANCEL_NORMAL` | 취소 거래가 정상적으로 완료된 상태. 금액 복구 및 기록 저장 완료 |
| `CANCEL_ERROR` | 취소 처리 중 오류가 발생한 상태. 재처리 가능 상태 |
| `CANCEL_PROC` | 취소 거래가 현재 처리 중인 상태. 중복 취소 방지 구간 |

### `ProcessResult` — 처리 결과 지시자

| 값 | 설명 |
| --- | --- |
| `PROCEED` | 정상 프로세스 진행. 신규 거래로 판단하여 로직 수행 |
| `DROP` | 중복 또는 불필요 전문 무시. 데이터 무결성을 위해 프로세스 즉시 종료 |
| `REPLY_ORIGINAL` | 기존 원거래 결과 재응답. 멱등성(Idempotency) 보장을 위해 저장된 데이터 회신 |
| `ERROR_NOT_FOUND` | 원거래 미존재 오류. 취소 요청 시 참조할 데이터가 없는 비정상 상황 |
| `SAVE_REVERSE` | 역전 거래 대비 선행 저장. 취소 전문이 원거래보다 먼저 도착한 경우 |

---

## 거래 유형별 처리 기준

### 1. 조회 (INQUIRY) 처리 기준

```java
/**
 * 1. 조회 (INQUIRY) 처리 기준
 *
 * [원거래 존재 + 상태 NORMAL / ERROR / PROCESSING]
 * - 동일한 거래 식별자를 가진 조회 요청이 이미 처리되었거나 처리 중인 상태이다.
 * - 시스템은 중복 요청으로 판단하고 추가 처리 없이 전문을 무시(DROP)한다.
 *
 * [원거래 미존재]
 * - 시스템에 동일 거래 식별자가 존재하지 않는 경우 최초 조회 요청으로 판단한다.
 * - 정상적인 조회 프로세스(PROCEED)를 수행한다.
 * * [상태 기록]
 * - NORMAL : 조회 성공 (데이터 반환 완료)
 * - ERROR  : 조회 실패 (계좌 없음, 외부 기관 응답 오류 등)
 * - ※ 중복 수신 시(DROP) 기존 상태를 유지하며 별도의 기록 갱신을 수행하지 않는다.
 */
```

---

### 2. 입금 (DEPOSIT) 처리 기준

```java
/**
 * 2. 입금 (DEPOSIT) 처리 기준
 *
 * [원거래 존재 + 상태 NORMAL / ERROR]
 * - 이미 최종 결과가 확정된 상태이므로 중복 입금 방지를 위해 기존 응답을 재전송(REPLY_ORIGINAL)한다.
 *
 * [원거래 존재 + 상태 PROCESSING]
 * - 현재 처리 중인 건이므로 동시성 제어를 위해 후속 전문은 무시(DROP)한다.
 *
 * [원거래 미존재]
 * - 신규 입금 요청으로 판단하여 프로세스를 시작(PROCEED)한다.
 *
 * [상태 기록]
 * - PROCESSING : SELECT FOR UPDATE 이후 즉시 기록 (방어막 형성)
 * - NORMAL     : 입금 확정 (원장 반영 완료)
 * - ERROR      : 입금 실패 (잔액 부족, 계좌 상태 이상 등 사유 포함 기록)
 */
```

---

### 3. 입금취소 (DEPOSIT_CANCEL) 처리 기준

```java
/**
 * 3. 입금취소 (DEPOSIT_CANCEL) 처리 기준
 *
 * [원거래 존재 + 상태 NORMAL / CANCEL_ERROR]
 * - 원거래가 성공했거나 이전 취소가 실패한 경우 재취소를 시도(PROCEED)한다.
 *
 * [원거래 존재 + 상태 CANCEL_PROC / PROCESSING]
 * - 현재 작업이 진행 중이므로 중복 진입을 차단하기 위해 무시(DROP)한다.
 *
 * [원거래 존재 + 상태 CANCEL_NORMAL]
 * - 이미 취소가 완료되었으므로 기존 성공 응답을 재전송(REPLY_ORIGINAL)한다.
 *
 * [원거래 미존재]
 *  취소 대상이 되는 입금 원거래가 시스템에 존재하지 않는 경우이다.
 *  정상적인 거래 흐름에서는 발생하면 안 되는 상황이다.
 *  시스템은 입금취소 정상 응답을 반환하고 입금취소 전문을 역거래(REVERSE) 형태로 저장한다.
 *  전문역거래 방어 (1. 입금취소 전문 수신 2. 입금 전문 수신)
 *
 * [상태 기록]
 * - CANCEL_PROC   : 취소 프로세스 진입 시 즉시 기록
 * - CANCEL_NORMAL : 자금 회수 및 취소 플래그 저장 완료
 * - CANCEL_ERROR  : 처리 중 기술적 오류(타임아웃 등) 발생 시 기록
 */
```

---

### 4. 출금 (WITHDRAW) 처리 기준

```java
/**
 * 4. 출금 (WITHDRAW) 처리 기준
 *
 * [원거래 존재]
 * - 출금은 금전적 리스크가 매우 크므로 동일 식별자 존재 시 무조건 무시(DROP)한다.
 * - 정정이 필요한 경우 출금취소를 이용해야 하며 동일 출금 거래 재처리는 불허한다.
 *
 * [원거래 미존재]
 * - 신규 출금 요청으로 판단하여 프로세스를 시작(PROCEED)한다.
 *
 * [상태 기록]
 * - PROCESSING : 거래 수신 및 로직 개시 시점 기록 (중복 출금 방어용)
 * - NORMAL     : 출금 정상 처리 (자금 차감 완료)
 * - ERROR      : 출금 처리 중 오류 발생 (한도 초과, 잔액 부족 등)
 */
```

---

### 5. 출금취소 (WITHDRAW_CANCEL) 처리 기준

```java
/**
 * 5. 출금취소 (WITHDRAW_CANCEL) 처리 기준
 *
 * [원거래 존재 + 상태 NORMAL / CANCEL_ERROR]
 * - 정상출금 건이거나 이전 취소 실패 건에 대해 취소 로직을 수행(PROCEED)한다.
 *
 * [원거래 존재 + 상태 CANCEL_NORMAL]
 * - 이미 취소된 건에 대해 기존 결과를 재응답(REPLY_ORIGINAL)하여 멱등성을 보장한다.
 *
 * [원거래 존재 + 상태 ERROR]
 * - 출금 자체가 실패했으므로 취소할 금액이 없다. 취소정상 응답코드를 저장하고 종료한다.
 *
 * [원거래 미존재]
 *  취소 대상이 되는 출금 원거래가 시스템에 존재하지 않는 경우이다.
 *  정상적인 거래 흐름에서는 발생하면 안 되는 상황이다.
 *  시스템은 출금취소 정상 응답을 반환하고 출금취소 전문을 역거래(REVERSE) 형태로 저장한다.
 *  전문역거래 방어 (1. 출금취소 전문 수신 2. 출금 전문 수신)
 *
 * [상태 기록]
 * - CANCEL_PROC   : 취소 시작 시 기록 (차감액 환원 프로세스 개시)
 * - CANCEL_NORMAL : 출금취소 확정 (잔액 복구 완료)
 * - CANCEL_ERROR  : 취소 처리 중 오류 (재시도 대상)
 * - SAVE_REVERSE  : 원거래 미존재 시 취소 전문 선행 저장 (운영 모니터링 대상)
 */
```

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
    │       ├─ REPLY_ORIGINAL  ──► 원거래 응답 재전송 (원거래응답 보장)
    │       ├─ ERROR_NOT_FOUND ──► 404 오류 응답
    │       └─ SAVE_REVERSE    ──► 역전 거래 선행 저장
    │
    └─ ResponseDto 반환
```

---

## 설계 원칙

### 1. 원거래응답 (Idempotency)

동일 전문이 재수신되면 **원거래 응답을 그대로 반환**합니다 (`REPLY_ORIGINAL`).

금융 전문 통신 환경에서는 다음과 같은 상황이 발생할 수 있습니다.

- 네트워크 지연
- 응답 패킷 유실
- 타임아웃 후 재전송
- 외부 기관의 자동 재시도

이 경우 동일 거래가 다시 처리되면 **중복 입금/출금과 같은 심각한 오류**가 발생할 수 있습니다.

따라서 시스템은 **기존에 저장된 거래 결과를 그대로 반환**하여  
거래 처리의 **멱등성(Idempotency)** 을 보장합니다.

---

### 2. 역전 거래 방어 (Reverse Transaction Guard)

출금취소 전문이 **출금 원거래보다 먼저 도착하는 상황**이 발생할 수 있습니다.

## 예시 상황

외부기관 → 출금취소 전문 전송(네트워크 지연)  외부기관 → 출금 전문 전송

이 경우 시스템에 원거래가 존재하지 않기 때문에  
단순 오류 처리하면 **거래 정합성이 깨질 수 있습니다.**

따라서 시스템은 다음과 같이 처리합니다.

1. 취소 전문을 **역전 거래(REVERSE)** 형태로 선행 저장 (`SAVE_REVERSE`)
2. 이후 출금 원거래가 도착하면
3. 저장된 취소 정보를 확인하여 거래 정합성을 맞춤

이 전략을 통해 **전문 수신 순서 역전 문제**를 방어합니다.

---

### 3. 비관적 락 (Pessimistic Locking)

동일 거래가 **동시에 처리되는 상황**을 방지하기 위해  
DB **비관적 락(Pessimistic Lock)** 을 사용합니다.

```sql
SELECT *
FROM TRANSACTION
WHERE TRX_ID = ?
FOR UPDATE NOWAIT
```
