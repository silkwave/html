# AGENTS.md

## Project Overview
This repository contains standalone HTML utility applications for various tasks including JSON parsing, calendar widgets, memo management, and SQL parameter conversion. All files are self-contained HTML documents with embedded CSS and JavaScript.

## Development Commands

### Local Development
```bash
# Start local development server (Python 3)
python -m http.server 8000

# Alternative with Node.js (if available)
npx serve .

# Open specific utility in browser
open parser.html  # macOS
xdg-open parser.html  # Linux
start parser.html  # Windows
```

### Validation and Linting
```bash
# HTML validation (requires vnu.jar)
java -jar vnu.jar --errors-only *.html

# CSS validation (requires css-validator)
css-validator *.html

# JavaScript linting (requires ESLint)
eslint *.html --ext .html

# Accessibility testing (requires axe-cli)
axe *.html
```

### Testing
```bash
# Manual testing checklist
echo "Testing checklist:"
echo "1. Dark theme toggle functionality"
echo "2. Korean language display"
echo "3. Responsive design on mobile"
echo "4. Cross-browser compatibility"
echo "5. File download/export features"

# Automated visual testing (requires playwright)
npx playwright test
```

## Code Style Guidelines

### HTML Structure
- Use semantic HTML5 elements (`<main>`, `<section>`, `<article>`, `<nav>`)
- Include proper DOCTYPE and meta tags
- Structure: `<!DOCTYPE html> <html> <head> <body>`
- Use Korean language attribute when needed: `<html lang="ko">`

### CSS Organization
```css
/* CSS Variables - Define at root */
:root {
  --primary-color: #4a90e2;
  --bg-dark: #1a1a1a;
  --text-light: #ffffff;
  --border-color: #333;
}

/* Dark theme support */
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #0d1117;
    --text-primary: #c9d1d9;
  }
}

/* Component structure */
.component-name {
  /* Layout */
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  
  /* Typography */
  font-family: 'Segoe UI', system-ui, sans-serif;
  
  /* Colors */
  background: var(--bg-dark);
  color: var(--text-light);
  
  /* Transitions */
  transition: all 0.3s ease;
}
```

### JavaScript Patterns
```javascript
// Use modern ES6+ features
const API_URL = 'https://api.example.com';

// Async/await for API calls
async function fetchData(url) {
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error('Network error');
    return await response.json();
  } catch (error) {
    console.error('Fetch failed:', error);
    showError('데이터를 가져올 수 없습니다');
  }
}

// DOM manipulation with event delegation
document.addEventListener('click', (e) => {
  if (e.target.matches('.btn-primary')) {
    handleButtonClick(e.target);
  }
});

// Module pattern for organization
const App = {
  init() {
    this.setupEventListeners();
    this.loadInitialState();
  },
  
  setupEventListeners() {
    // Event listeners setup
  },
  
  loadInitialState() {
    // Initial state loading
  }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
```

### File Naming Conventions
- Use kebab-case for HTML files: `sql-param-converter.html`
- Keep filenames descriptive but concise
- Avoid spaces or special characters
- Use English names for consistency

### Comment Standards
```html
<!-- HTML Comments - Use for structure -->
<!-- Main Application Container -->
<main class="app-container">
```

```css
/* CSS Comments - Group related styles */
/* ========================================
   DARK THEME STYLES
   ======================================== */
```

```javascript
// JavaScript Comments - Use JSDoc for functions
/**
 * Parses JSON data and flattens nested objects
 * @param {Object} data - The JSON data to parse
 * @returns {Array} Flattened array of objects
 */
function parseJSON(data) {
  // Implementation
}
```

## Project-Specific Standards

### Dark Theme Implementation
- Always support dark theme via CSS variables
- Use `prefers-color-scheme: dark` media query
- Include manual theme toggle for user preference
- Store theme choice in localStorage

### Korean Language Support
- Use UTF-8 encoding: `<meta charset="UTF-8">`
- Include Korean language attribute when content is primarily Korean
- Use appropriate fonts for Korean text display
- Test with Korean input methods and text rendering

### Standalone HTML File Structure
```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Utility Name</title>
  <style>
    /* All CSS styles here */
  </style>
</head>
<body>
  <!-- HTML structure -->
  <script>
    // All JavaScript code here
  </script>
</body>
</html>
```

### Cross-Browser Compatibility
- Test in Chrome, Firefox, Safari, Edge
- Use feature detection for modern APIs
- Provide fallbacks for older browsers
- Avoid experimental features without polyfills

### Accessibility Requirements
- Use semantic HTML elements
- Include ARIA labels where needed
- Ensure keyboard navigation support
- Provide sufficient color contrast
- Test with screen readers

### Performance Guidelines
- Minimize DOM manipulation
- Use event delegation for dynamic content
- Optimize images and assets
- Implement lazy loading where appropriate
- Keep JavaScript execution time under 100ms

## File Organization Standards

### Directory Structure
```
/
├── main.html              # Navigation hub
├── parser.html            # JSON parsing utility
├── calendar.html          # Calendar widget
├── memo.html              # Memo application
├── TextDecoder.html       # Encoding detection
├── sql-param-converter.html # SQL parameter converter
└── AGENTS.md              # This file
```

### Component Patterns
- Each HTML file should be self-contained
- Use consistent CSS class naming across files
- Implement similar UI patterns for familiarity
- Include error handling and user feedback

### Data Handling
- Use localStorage for persistent data
- Implement proper data validation
- Include export/import functionality where applicable
- Handle file operations with proper error messages

## Security Considerations
- Sanitize user inputs before processing
- Use safe innerHTML alternatives (textContent, innerText)
- Implement proper file upload validation
- Avoid eval() and other dangerous functions
- Use HTTPS for any external API calls

## Browser Testing Matrix
- Chrome 90+ (Primary development)
- Firefox 88+ (Secondary testing)
- Safari 14+ (iOS compatibility)
- Edge 90+ (Windows support)
- Mobile browsers (Responsive testing)

## Common Issues and Solutions

### Korean Text Rendering
- Issue: Korean text not displaying properly
- Solution: Ensure proper UTF-8 encoding and font-family settings

### Dark Theme Glitches
- Issue: Theme not applying consistently
- Solution: Use CSS variables and test all color combinations

### Mobile Responsiveness
- Issue: Layout breaking on small screens
- Solution: Use CSS Grid/Flexbox with proper breakpoints

### File Download Issues
- Issue: Downloads not working in some browsers
- Solution: Use Blob URLs and test cross-browser compatibility