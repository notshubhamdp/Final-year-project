(function () {
  if (window.__nivasaSupportWidgetLoaded) {
    return;
  }
  window.__nivasaSupportWidgetLoaded = true;

  var API = {
    ask: "/api/chatbot/ask?q=",
    faqs: "/api/chatbot/faqs"
  };

  function create(tag, cls, text) {
    var el = document.createElement(tag);
    if (cls) {
      el.className = cls;
    }
    if (text) {
      el.textContent = text;
    }
    return el;
  }

  function addMessage(container, text, type) {
    var msg = create("div", "chatbot-msg " + type, text);
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
    return msg;
  }

  function addSupportEmail(container, email) {
    if (!email) {
      return;
    }

    var msg = create("div", "chatbot-msg bot");
    var prefix = document.createTextNode("Customer care email: ");
    var link = create("a", "chatbot-email-link", email);
    link.href = "mailto:" + email;
    msg.appendChild(prefix);
    msg.appendChild(link);
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
  }

  function setSuggestions(host, items, askFn) {
    host.innerHTML = "";
    if (!items || !items.length) {
      return;
    }

    var wrap = create("div", "chatbot-suggestions");
    items.slice(0, 6).forEach(function (item) {
      var chip = create("button", "chatbot-chip", item);
      chip.type = "button";
      chip.addEventListener("click", function () {
        if (item === "Open FAQ page") {
          window.location.href = "/faq";
          return;
        }
        askFn(item);
      });
      wrap.appendChild(chip);
    });
    host.appendChild(wrap);
  }

  function init() {
    var toggle = create("button", "chatbot-toggle");
    toggle.type = "button";
    toggle.setAttribute("aria-label", "Open customer care support");
    var toggleIcon = create("span", "chatbot-toggle-icon");
    toggleIcon.setAttribute("aria-hidden", "true");
    toggleIcon.innerHTML = ''
      + '<svg viewBox="0 0 72 72" xmlns="http://www.w3.org/2000/svg">'
      + '<defs>'
      + '<linearGradient id="supportBg" x1="0%" y1="0%" x2="100%" y2="100%">'
      + '<stop offset="0%" stop-color="#7dd3fc"></stop>'
      + '<stop offset="100%" stop-color="#38bdf8"></stop>'
      + '</linearGradient>'
      + '<linearGradient id="supportBubble" x1="0%" y1="0%" x2="100%" y2="100%">'
      + '<stop offset="0%" stop-color="#fb7185"></stop>'
      + '<stop offset="100%" stop-color="#f97316"></stop>'
      + '</linearGradient>'
      + '</defs>'
      + '<path d="M36 6c-14.8 0-26.8 11.9-26.8 26.7 0 9.4 4.9 18.2 13 23.1L36 68l13.8-12.2c8.1-4.9 13-13.7 13-23.1C62.8 17.9 50.8 6 36 6z" fill="url(#supportBg)"></path>'
      + '<circle cx="36" cy="30.5" r="18.5" fill="#ffffff" fill-opacity=".24"></circle>'
      + '<circle cx="36" cy="32" r="13.8" fill="#fbbf24"></circle>'
      + '<path d="M26.2 31.2c0-5.7 4.4-10.3 9.8-10.3s9.8 4.6 9.8 10.3v2.7c0 1.2-.9 2.1-2 2.1h-1.3c-1.1 0-2-.9-2-2.1V31c0-2.4-2-4.4-4.5-4.4s-4.5 2-4.5 4.4v2.9c0 1.2-.9 2.1-2 2.1h-1.3c-1.1 0-2-.9-2-2.1v-2.7z" fill="#4b5563"></path>'
      + '<rect x="23.4" y="31" width="5.8" height="10.8" rx="2.6" fill="#4b5563"></rect>'
      + '<rect x="42.8" y="31" width="5.8" height="10.8" rx="2.6" fill="#4b5563"></rect>'
      + '<path d="M30.2 47.2c1.9 1.7 3.7 2.4 5.8 2.4s3.9-.7 5.8-2.4" fill="none" stroke="#8b5a2b" stroke-width="2" stroke-linecap="round"></path>'
      + '<circle cx="31.6" cy="37.8" r="1.3" fill="#7c2d12"></circle>'
      + '<circle cx="40.4" cy="37.8" r="1.3" fill="#7c2d12"></circle>'
      + '<rect x="48" y="11" width="17" height="13" rx="3.2" fill="url(#supportBubble)"></rect>'
      + '<path d="M52.2 24l3.1-3.4h9.7" fill="url(#supportBubble)"></path>'
      + '<circle cx="53.8" cy="17.5" r="1.6" fill="#ffffff"></circle>'
      + '<circle cx="59" cy="17.5" r="1.6" fill="#ffffff"></circle>'
      + '<circle cx="64.2" cy="17.5" r="1.6" fill="#ffffff"></circle>'
      + '</svg>';
    toggle.appendChild(toggleIcon);

    var panel = create("div", "chatbot-panel");
    var header = create("div", "chatbot-header");
    var titleWrap = create("div", "chatbot-title-wrap");
    var title = create("div", "chatbot-title", "Customer Care Support");
    var subtitle = create("div", "chatbot-subtitle", "FAQ answers only");
    var close = create("button", "chatbot-close", "x");
    close.type = "button";

    titleWrap.appendChild(title);
    titleWrap.appendChild(subtitle);
    header.appendChild(titleWrap);
    header.appendChild(close);

    var body = create("div", "chatbot-body");
    var footer = create("div", "chatbot-footer");
    var suggestionHost = create("div", "chatbot-suggestion-host");

    var form = create("form", "chatbot-form");
    var inputWrap = create("div", "chatbot-input-wrap");
    var inputIcon = create("span", "chatbot-input-icon");
    inputIcon.setAttribute("aria-hidden", "true");
    inputIcon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"></circle><path d="M20 20l-3.5-3.5"></path></svg>';
    var input = create("input", "chatbot-input");
    input.type = "text";
    input.placeholder = "Search your support question...";
    input.required = true;
    input.setAttribute("aria-label", "Search customer support questions");
    var send = create("button", "chatbot-send", "Ask");
    send.type = "submit";

    inputWrap.appendChild(inputIcon);
    inputWrap.appendChild(input);
    form.appendChild(inputWrap);
    form.appendChild(send);

    panel.appendChild(header);
    panel.appendChild(body);
    footer.appendChild(suggestionHost);
    footer.appendChild(form);
    panel.appendChild(footer);
    document.body.appendChild(panel);
    document.body.appendChild(toggle);

    var initialized = false;
    var supportEmail = "nivasacontact@gmail.com";
    var faqQuestions = [];

    function openPanel() {
      panel.classList.add("open");
      input.focus();
    }

    function closePanel() {
      panel.classList.remove("open");
    }

    function welcomeMessage() {
      addMessage(
        body,
        "Welcome to NIVASA customer care. Ask your question or choose a common question below. This widget does not use AI and only shows fixed FAQ answers.",
        "bot"
      );
    }

    function loadFaqCatalog() {
      return fetch(API.faqs, {
        method: "GET",
        headers: { Accept: "application/json" }
      })
        .then(function (response) { return response.json(); })
        .then(function (data) {
          if (!data || !data.success) {
            throw new Error("Support FAQs unavailable");
          }
          supportEmail = data.supportEmail || supportEmail;
          faqQuestions = Array.isArray(data.faqs)
            ? data.faqs.map(function (item) { return item.question; })
            : [];
        });
    }

    function ensureReady() {
      if (initialized) {
        return Promise.resolve();
      }

      body.innerHTML = "";
      suggestionHost.innerHTML = "";
      welcomeMessage();

      return loadFaqCatalog()
        .then(function () {
          var suggestions = faqQuestions.slice(0, 5);
          suggestions.push("Open FAQ page");
          setSuggestions(suggestionHost, suggestions, ask);
          initialized = true;
        })
        .catch(function () {
          addMessage(body, "Support questions are unavailable right now.", "bot");
          addSupportEmail(body, supportEmail);
          setSuggestions(suggestionHost, ["Open FAQ page"], ask);
          initialized = true;
        });
    }

    function ask(question) {
      var q = (question || "").trim();
      if (!q) {
        return;
      }

      addMessage(body, q, "user");

      fetch(API.ask + encodeURIComponent(q), {
        method: "GET",
        headers: { Accept: "application/json" }
      })
        .then(function (response) { return response.json(); })
        .then(function (data) {
          if (!data || !data.success) {
            throw new Error("Support reply unavailable");
          }

          supportEmail = data.supportEmail || supportEmail;
          addMessage(body, data.answer || "No support answer available.", "bot");
          if (data.matched === false) {
            addSupportEmail(body, supportEmail);
          }

          var suggestions = Array.isArray(data.suggestions) ? data.suggestions.slice() : [];
          if (suggestions.indexOf("Open FAQ page") === -1) {
            suggestions.push("Open FAQ page");
          }
          setSuggestions(suggestionHost, suggestions, ask);
        })
        .catch(function () {
          addMessage(body, "We could not load a support answer right now.", "bot");
          addSupportEmail(body, supportEmail);
          setSuggestions(suggestionHost, ["Open FAQ page"], ask);
        });
    }

    toggle.addEventListener("click", function () {
      if (panel.classList.contains("open")) {
        closePanel();
      } else {
        openPanel();
        ensureReady();
      }
    });

    close.addEventListener("click", closePanel);

    form.addEventListener("submit", function (event) {
      event.preventDefault();
      var q = input.value;
      input.value = "";
      ask(q);
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
