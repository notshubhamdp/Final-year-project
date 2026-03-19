/**
 * Loading Animation Manager
 * Handles showing and hiding loading overlays throughout the application
 */

class LoadingAnimationManager {
  constructor() {
    this.loadingOverlay = null;
    this.isVisible = false;
    this.init();
  }

  /**
   * Initialize the loading overlay on page load
   */
  init() {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => this.createOverlay());
    } else {
      this.createOverlay();
    }
  }

  /**
   * Create the loading overlay HTML structure
   */
  createOverlay() {
    if (this.loadingOverlay) return;

    this.loadingOverlay = document.createElement('div');
    this.loadingOverlay.className = 'loading-overlay hidden';
    this.loadingOverlay.innerHTML = `
      <div class="spinner-container">
        <div class="logo-spinner">
          <img src="/images/nivasa-logo.svg" alt="NIVASA" class="logo-image">
        </div>
        <div class="loading-text">
          Loading<span class="dot-animation"></span>
        </div>
      </div>
    `;
    document.body.appendChild(this.loadingOverlay);
  }

  /**
   * Show the loading overlay
   * @param {string} message - Optional custom loading message
   */
  show(message = 'Loading') {
    if (!this.loadingOverlay) this.createOverlay();
    
    if (message) {
      const loadingText = this.loadingOverlay.querySelector('.loading-text');
      if (loadingText) {
        loadingText.textContent = message;
      }
    }

    this.loadingOverlay.classList.remove('hidden');
    this.isVisible = true;
  }

  /**
   * Hide the loading overlay
   */
  hide() {
    if (this.loadingOverlay) {
      this.loadingOverlay.classList.add('hidden');
      this.isVisible = false;
    }
  }

  /**
   * Toggle the loading overlay visibility
   * @param {boolean} visible - True to show, false to hide
   * @param {string} message - Optional custom message
   */
  toggle(visible, message = null) {
    if (visible) {
      this.show(message);
    } else {
      this.hide();
    }
  }
}

// Create global instance
const loader = new LoadingAnimationManager();

/**
 * Auto-show loading on form submissions
 */
document.addEventListener('DOMContentLoaded', () => {
  document.addEventListener('submit', (e) => {
    const form = e.target;
    // Don't show loading for search or filter forms
    if (!form.classList.contains('no-loading')) {
      loader.show('Processing...');
    }
  });

  /**
   * Auto-show loading on link/button clicks
   */
  document.addEventListener('click', (e) => {
    const target = e.target.closest('a, button, [role="button"]');
    
    if (!target) return;
    
    // Skip if element has no-loading class or is disabled
    if (target.classList.contains('no-loading') || target.disabled) {
      return;
    }
    
    // Skip if it's a download link or has download attribute
    if (target.hasAttribute('download')) {
      return;
    }
    
    // Skip hash links (#) - they don't navigate
    if (target.href && target.href.includes('#') && !target.href.includes('?')) {
      return;
    }
    
    // Skip external links (different domain)
    if (target.href && target.href.startsWith('http') && !target.href.includes(window.location.hostname)) {
      return;
    }
    
    // Skip if it's a modal trigger or popup
    if (target.hasAttribute('data-bs-toggle') || target.hasAttribute('data-toggle') || target.hasAttribute('onclick')) {
      return;
    }
    
    // Show loading for navigation links and buttons
    if (target.href || target.type === 'submit') {
      loader.show('Loading...');
    }
  }, true); // Use capture phase to catch clicks early
});

/**
 * Auto-show loading on navigation
 */
window.addEventListener('beforeunload', () => {
  if (!sessionStorage.getItem('skipLoading')) {
    loader.show('Loading...');
  }
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
  module.exports = LoadingAnimationManager;
}
