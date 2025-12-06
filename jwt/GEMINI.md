## 프로젝트 개요

이 프로젝트는 JWT(JSON Web Token) 처리, 인증 로직, 머신러닝 모델 컴포넌트를 포함하는 다목적 코드베이스입니다. 주요 기능은 다음과 같습니다.

- **클라이언트 측 JWT 유틸리티**: 브라우저 내에서 JWT를 디코딩, 서명, 검증하는 독립적인 HTML 도구입니다.
- **인증 핸들러**: Python으로 작성된 OAuth2 및 OpenID Connect 인증 흐름을 관리하는 로직입니다.
- **머신러닝 컴포넌트**: PyTorch로 구현된 어텐션(attention) 메커니즘을 포함한 신경망 모델 코드입니다.
- **고성능 컴퓨팅**: SIMD(Single Instruction, Multiple Data) 명령어를 사용하여 C++로 구현된 성능 최적화 코드입니다.

---

## 주요 컴포넌트

### 1. Client-Side JWT 유틸리티 (`jwt4.html`)

`jwt4.html` 파일은 브라우저에서 직접 JWT를 생성, 디코딩, 서명 및 검증할 수 있는 독립적인 웹 애플리케이션입니다. 주요 기능은 다음과 같습니다.

- **지원 알고리즘**: HS256/384/512, RS256, ES256
- **주요 기능**:
  - JWT 디코딩 및 JSON 편집기
  - HMAC(secret key), RSA/ECDSA(private/public key)를 사용한 서명 및 검증
  - 개인키 파일 업로드 (브라우저 내에서만 처리)
  - 토큰 만료 시간(`exp`) 카운트다운
  - 다크/라이트 테마 지원
- **구현**: 모든 기능은 클라이언트 측 JavaScript와 Web Crypto API를 사용하여 구현되었으며, 서버와의 통신이 없습니다.

### 2. 인증 로직 (Python)

Python으로 작성된 인증 관련 코드는 OAuth2 및 OpenID Connect 프로토콜을 처리하는 `AuthHandler` 클래스를 중심으로 구성됩니다.

- **주요 기능**:
  - 인증 URI 생성
  - 인증 응답을 받아 액세스 토큰으로 교환
  - OpenID Connect 스킴 및 자격 증명 관리
  - 유닛 테스트를 통해 기능의 정확성 보장

### 3. 머신러닝 모델 (PyTorch)

PyTorch를 기반으로 한 음성-텍스트 변환(Speech-to-Text) 모델의 일부로 보이는 어텐션 메커니즘 구현 코드가 포함되어 있습니다.

- **주요 컴포넌트**:
  - `AttForward`, `AttLoc` 등의 어텐션 메커니즘을 구현한 `forward` 함수
  - 인코더-디코더 구조, 마스킹, 가중치 합산(weighted sum) 등의 기법을 사용
  - TTS(Text-to-Speech)를 위한 Monotonic Attention 제약 조건 적용

### 4. 고성능 컴퓨팅 (C++/SIMD)

신경망 평가를 위한 고성능 연산을 위해 C++과 SIMD 내장 함수(intrinsics)를 사용한 코드가 포함되어 있습니다.

- **사용된 기술**: AVX512, AVX2, SSSE3 등의 SIMD 명령어셋
- **주요 기능**:
  - 행렬 곱셈과 같은 연산을 효율적으로 처리하기 위한 수평 덧셈(`hadd`) 및 인터리브(interleave) 연산
  - 대규모 입력 차원에 대한 성능 최적화

---

*이 문서는 프로젝트의 다양한 코드 스니펫과 `jwt4.html` 파일 분석을 기반으로 생성되었습니다.*
