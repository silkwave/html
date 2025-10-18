# Dev Dark — 카드 컨트롤 문서

파일: `dev_dark.html`

이 문서는 페이지 로드 시 첫 번째 카드만 보이도록 하는 단일 카드 뷰와 Prev/Next/모두 보기 컨트롤의 사용법과 구현 세부사항을 설명합니다.

## 기능 요약
- 초기 로드: 첫 번째 `.site-card`만 화면에 보입니다.
- Prev/Next: 왼쪽 하단의 ◀ / ▶ 버튼으로 카드 간 전환합니다.
- 모두 보기: '모두 보기' 버튼을 눌러 원래의 멀티카드(플렉스) 레이아웃을 표시합니다. 다시 누르면 단일 카드 뷰로 돌아갑니다.

## HTML 변경 요약
- 페이지 바디에 고정 위치의 컨트롤을 추가했습니다:

```html
<div id="cardControls" style="position:fixed;left:20px;bottom:20px;display:flex;gap:8px;z-index:40;">
  <button id="prevCard" aria-label="이전 카드">◀</button>
  <button id="nextCard" aria-label="다음 카드">▶</button>
  <button id="toggleAll" aria-pressed="false">모두 보기</button>
</div>
```

## 주요 구현 포인트 (JavaScript)
- 위치: 기존 스크립트 블록 상단에 IIFE로 추가
- 핵심 동작:
  - cards 배열에 `.site-card` 요소들을 수집
  - index 변수로 현재 표시 중인 카드 인덱스 관리
  - `updateView()` 함수
    - `showAll`이 true면 `.site-wrapper`를 flex로, 모든 카드를 display:block으로 보이게 하고 Prev/Next를 비활성화
    - `showAll`이 false면 `.site-wrapper`를 block으로 바꾸고 현재 인덱스의 카드만 보이게 함
  - Prev/Next 버튼은 인덱스 범위를 벗어나지 않게 보호

주요 코드(요약):

```js
const cards = Array.from(document.querySelectorAll('.site-card'));
let index = 0;
let showAll = false;

function updateView() {
  if (showAll) { /* flex + 모두 보이기 */ }
  else { /* 단일 카드만 보이기 */ }
}

prevBtn.addEventListener('click', ...);
nextBtn.addEventListener('click', ...);
toggleAll.addEventListener('click', ...);

updateView(); // 초기화
```

## 접근성 및 UX
- 컨트롤은 `aria-label`과 `aria-pressed`를 사용해 스크린리더와의 상호작용을 고려했습니다.
- Prev/Next의 disabled 상태는 현재 인덱스에 따라 적용됩니다.

## 테스트 및 확인 방법
1. 정적 서버에서 실행:

```bash
python3 -m http.server 8000
```
2. 브라우저에서 `http://localhost:8000/devhtml/dev_dark_refactored.html` 열기
3. 초기 로드시 첫 카드만 보이는지 확인
4. Next 버튼 클릭 → 두 번째 카드로 이동, Prev 버튼 확인
5. 모두 보기 클릭 → 세 카드 모두 보이는지 확인

## 권장 개선 사항
- 카드 전환 애니메이션(페이드/슬라이드) 추가
- 키보드 화살표(←/→)로 카드 전환 허용
- '모두 보기' 상태를 localStorage에 저장해 재방문 시 동일한 뷰 유지
---

