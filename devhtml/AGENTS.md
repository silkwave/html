# AGENTS.md - 개발 에이전트 가이드

## 빌드/테스트/실행 명령어

```bash
# 로컬 개발 서버 실행 (Python 3)
python3 -m http.server 8000

# 로컬 개발 서버 실행 (Node.js - http-server 사용)
npx http-server . -p 8000

# 로컬 개발 서버 실행 (PHP 내장 서버)
php -S localhost:8000

# HTML 유효성 검사 (단일 파일)
curl -s -F "uploaded_file=@dev_dark.html;type=text/html" https://validator.w3.org/nu/

# CSS 유효성 검사 (단일 파일)
curl -s -F "file=@dev_dark.html;type=text/css" https://jigsaw.w3.org/css-validator/validator

# 링크 유효성 검사 (단일 URL)
curl -s -o /dev/null -w "%{http_code}" https://github.com

# 브라우저에서 직접 열기
# file:///home/silkw/apps/html/devhtml/dev_dark.html

# Live Reload (VS Code Live Server 확장장 권장)
# F1 → Live Server: Open with Live Server
```

## 기술 스택

- **HTML5**: 시맨틱 마크업, 라벨 및 접근성 속성 사용
- **CSS3**: CSS 변수, Flexbox, Grid, 미디어 쿼리, 다크모드 지원
- **JavaScript**: Vanilla JS (ES6+), IIFE 패턴, LocalStorage API
- **데이터 저장**: 브라우저 LocalStorage (클라이언트 측)
- **스타일링**: iOS 디자인 시스템 및 E-ink 테마

## 코드 스타일 가이드라인

### 1. 파일 구조 및 네이밍

```
devhtml/
├── dev_dark.html              # 메인 다크테마 파일
├── devsiteInk.html           # E-ink 테마 파일
├── dev_dark_refactored.html  # 리팩토링 버전
├── GEMINI.md                  # 프로젝트 설정
└── AGENTS.md                  # 이 파일
```

- 파일명: kebab-case (소문자, 하이픈 연결)
- 클래스명: kebab-case (site-card, link-button)
- ID명: camelCase (cardControls, prevCard)
- 변수명: camelCase (showAll, currentIndex)

### 2. HTML 작성 규칙

```html
<!-- 언어 및 메타 태그 -->
<html lang="ko">
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />

<!-- 시맨틱 태그 사용 -->
<main class="site-wrapper">
  <section class="site-card">
    <h3>제목</h3>
    <p>설명</p>
    <ul class="link-list">
      <li><a class="link-button" href="" target="_blank" rel="noopener">링크</a></li>
    </ul>
    <textarea class="card-input" placeholder="" aria-label=""></textarea>
  </section>
</main>

<!-- 접근성 속성 필수 -->
<button id="toggleAll" aria-pressed="false">모두 보기</button>
<textarea aria-label="개발 Sites memo"></textarea>
```

### 3. CSS 작성 규칙

#### CSS 변수 (우선순위)
```css
:root {
  /* 다크모드 변수 */
  --bg-light: #f5f5f7;
  --bg-dark: #000000;
  --card-light: #ffffff;
  --card-dark: #1c1c1e;
  --accent: #007AFF;
  --muted: #86868b;
  --separator: #e0e0e2;
}
```

#### 스타일 섹션 구조
```css
/* =======================
   섹션 제목 (한글)
   ======================= */

/* 기본 스타일 */
* { box-sizing: border-box; }

/* 반응형 및 미디어 쿼리 */
@media (max-width: 768px) { }

/* 다크모드 지원 */
@media (prefers-color-scheme: dark) { }

/* Safe Area (노치 대응) */
@supports (padding: max(0px)) { }
```

### 4. JavaScript 작성 규칙

#### IIFE 패턴 사용
```javascript
(function() {
  'use strict';
  
  // DOM 요소 참조
  const cards = Array.from(document.querySelectorAll('.site-card'));
  const wrapper = document.querySelector('.site-wrapper');
  
  // 상태 변수
  let index = 0;
  let showAll = false;
  
  // 함수 정의
  function updateView() { }
  
  // 이벤트 리스너
  document.addEventListener('DOMContentLoaded', updateView);
})();
```

#### LocalStorage 사용 패턴
```javascript
// 디바운스 적용 저장
function bindDebouncedSave(el, key, delay = 300) {
  el.value = localStorage.getItem(key) || "";
  let timeoutId;
  
  el.addEventListener("input", () => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => {
      localStorage.setItem(key, el.value);
    }, delay);
  });
}

// 키 생성 규칙
const storageKey = `card-memo-${cardTitle}`;  // card-memo-개발 Sites
const einkStorageKey = `memo-card-eink-${i}`; // memo-card-eink-0
```

### 5. 이벤트 처리

```javascript
// 버튼 이벤트
prevBtn.addEventListener('click', () => {
  if (index > 0) {
    index--;
    updateView();
  }
});

// 키보드 이벤트 (접근성)
document.addEventListener('keydown', (e) => {
  if (showAll || document.activeElement.tagName === 'TEXTAREA') return;
  
  switch(e.key) {
    case 'ArrowLeft': prevBtn.click(); break;
    case 'ArrowRight': nextBtn.click(); break;
  }
});

// 터치 이벤트 (모바일 최적화)
element.addEventListener('touchstart', handler);
element.addEventListener('touchend', handler);
```

### 6. 디자인 시스템

#### iOS 스타일 (dev_dark.html)
- 카드 둥근 모서리: 20px
- 액센트 색상: #007AFF
- 버튼 높이: 44px 최소 (터치 타겟)
- 애니메이션: 0.3s ease

#### E-ink 스타일 (devsiteInk.html)
- 테두리: 2px solid #1a1a1a
- 그림자: 4px 4px 0px #1a1a1a
- 폰트: 명조체 (Serif) 우선
- 배경: 노이즈 텍스처 적용

### 7. 접근성 가이드

```html
<!-- ARIA 속성 -->
<button aria-pressed="false">모두 보기</button>
<textarea aria-label="개발 Sites memo"></textarea>

<!-- 키보드 네비게이션 지원 -->
<a href="#" target="_blank" rel="noopener">외부 링크</a>

<!-- 포커스 스타일 -->
:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}
```

### 8. 모바일 최적화

```css
/* 뷰포트 설정 */
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />

/* 터치 최적화 */
-webkit-tap-highlight-color: transparent;
user-select: none;

/* Safe Area 대응 */
@supports (padding: max(0px)) {
  body {
    padding-left: max(0px, env(safe-area-inset-left));
    padding-right: max(0px, env(safe-area-inset-right));
  }
}
```

### 9. 에러 처리 및 디버깅

```javascript
// 콘솔 로깅
console.log('카드 인덱스:', index);
console.error('에러 발생:', error);

// LocalStorage 오류 처리
try {
  localStorage.setItem(key, value);
} catch (e) {
  console.warn('LocalStorage 저장 실패:', e);
}

// DOM 요소 존재 확인
if (!document.querySelector('.site-card')) {
  console.error('카드 요소를 찾을 수 없습니다');
}
```

### 10. 성능 최적화

```javascript
// DOM 조작 최소화
Object.assign(element.style, {
  display: 'block',
  opacity: '1'
});

// 이벤트 위임 사용
wrapper.addEventListener('click', (e) => {
  if (e.target.classList.contains('link-button')) {
    // 링크 처리
  }
});

// SVG 노이즈 텍스처 최적화
const optimizedSvg = `<svg viewBox='0 0 250 250'><filter id='n'><feTurbulence baseFrequency='0.7'/></filter><rect width='100%' height='100%' filter='url(#n)' opacity='0.05'/></svg>`;
```

## 주의사항

- **언어**: 모든 UI 텍스트는 한국어로 작성
- **데이터 저장**: LocalStorage만 사용 (서버 없음)
- **브라우저 호환성**: 최신 브라우저 (ES6+, CSS 변수)
- **보안**: 외부 링크에는 `rel="noopener"` 필수
- **접근성**: 키보드 네비게이션 및 스크린리더 지원
- **모바일**: 터치 이벤트 및 Safe Area 대응
- **테마**: 시스템 다크모드 자동 감지 지원

## 파일 수정 시 체크리스트

- [ ] HTML 시맨틱 태그 올바르게 사용
- [ ] CSS 변수 일관성 유지
- [ ] JavaScript IIFE 패턴 적용
- [ ] 접근성 속성 추가 (aria-label, aria-pressed)
- [ ] 모바일 반응형 확인
- [ ] LocalStorage 키 네이밍 규칙 준수
- [ ] 외부 링크에 target="_blank" rel="noopener" 추가
- [ ] 다크모드 스타일 확인
- [ ] 콘솔 오류 없는지 확인