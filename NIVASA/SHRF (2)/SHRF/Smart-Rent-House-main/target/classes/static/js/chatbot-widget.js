(function () {
  if (window.__nivasaAiChatLoaded) {
    return;
  }
  window.__nivasaAiChatLoaded = true;

  var STORAGE_KEY = "nivasa_ai_chat_key";
  var API = {
    start: "/api/ai-chat/start",
    message: "/api/ai-chat/message",
    history: "/api/ai-chat/history?conversationKey="
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
  }

  function setSuggestions(host, items, askFn) {
    host.innerHTML = "";
    if (!items || !items.length) {
      return;
    }
    var wrap = create("div", "chatbot-suggestions");
    items.slice(0, 4).forEach(function (item) {
      var chip = create("button", "chatbot-chip", item);
      chip.type = "button";
      chip.addEventListener("click", function () {
        askFn(item);
      });
      wrap.appendChild(chip);
    });
    host.appendChild(wrap);
  }

  function getSessionId() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID();
    }
    return "session-" + Date.now();
  }

  function init() {
    var toggle = create("button", "chatbot-toggle", "AI");
    toggle.setAttribute("aria-label", "Open AI chatbot");

    var panel = create("div", "chatbot-panel");
    var header = create("div", "chatbot-header");
    var title = create("div", "chatbot-title", "NIVASA AI Assistant");
    var close = create("button", "chatbot-close", "×");
    close.type = "button";

    header.appendChild(title);
    header.appendChild(close);

    var body = create("div", "chatbot-body");
    var suggestionHost = create("div");

    var form = create("form", "chatbot-form");
    var input = create("input", "chatbot-input");
    input.type = "text";
    input.placeholder = "Ask anything about NIVASA...";
    input.required = true;
    var send = create("button", "chatbot-send", "Send");
    send.type = "submit";

    form.appendChild(input);
    form.appendChild(send);

    panel.appendChild(header);
    panel.appendChild(body);
    panel.appendChild(suggestionHost);
    panel.appendChild(form);
    document.body.appendChild(panel);
    document.body.appendChild(toggle);

    var conversationKey = localStorage.getItem(STORAGE_KEY) || "";
    var initialized = false;

    function openPanel() {
      panel.classList.add("open");
      input.focus();
    }

    function closePanel() {
      panel.classList.remove("open");
    }

    function startConversation() {
      return fetch(API.start, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId: getSessionId() })
      })
        .then(function (r) { return r.json(); })
        .then(function (data) {
          if (!data || !data.success || !data.conversationKey) {
            throw new Error("Failed to start chat");
          }
          conversationKey = data.conversationKey;
          localStorage.setItem(STORAGE_KEY, conversationKey);
          return conversationKey;
        });
    }

    function loadHistory() {
      if (!conversationKey) {
        return Promise.resolve();
      }
      return fetch(API.history + encodeURIComponent(conversationKey), {
        method: "GET",
        headers: { "Accept": "application/json" }
      })
        .then(function (r) { return r.json(); })
        .then(function (data) {
          if (!data || !data.success || !Array.isArray(data.history)) {
            return;
          }
          body.innerHTML = "";
          data.history.forEach(function (item) {
            var role = (item.role || "").toUpperCase();
            addMessage(body, item.content || "", role === "USER" ? "user" : "bot");
          });
        })
        .catch(function () {
          // Ignore history errors.
        });
    }

    function ensureReady() {
      if (initialized) {
        return Promise.resolve();
      }

      var chain = Promise.resolve();
      if (!conversationKey) {
        chain = chain.then(startConversation);
      }
      return chain.then(loadHistory).then(function () {
        if (!body.children.length) {
          addMessage(body, "Hello. I am your AI assistant for NIVASA. Ask me about account, listings, visits, or payments.", "bot");
          setSuggestions(suggestionHost, [
            "How to register",
            "How to list property",
            "How to contact landlord",
            "Payment help"
          ], ask);
        }
        initialized = true;
      }).catch(function () {
        addMessage(body, "Chat service is currently unavailable. Please try again later.", "bot");
      });
    }

    function ask(question) {
      var q = (question || "").trim();
      if (!q) {
        return;
      }
      addMessage(body, q, "user");

      var sendReq = function () {
        return fetch(API.message, {
          method: "POST",
          headers: { "Content-Type": "application/json", "Accept": "application/json" },
          body: JSON.stringify({
            conversationKey: conversationKey,
            message: q
          })
        })
          .then(function (r) { return r.json(); })
          .then(function (data) {
            if (!data || !data.success) {
              var err = data && data.error ? data.error : "Could not process your request.";
              addMessage(body, err, "bot");
              return;
            }
            addMessage(body, data.answer || "No response generated.", "bot");
            setSuggestions(suggestionHost, data.suggestions || [], ask);
          })
          .catch(function () {
            addMessage(body, "Service unavailable. Please try again.", "bot");
          });
      };

      if (!conversationKey) {
        startConversation().then(sendReq).catch(function () {
          addMessage(body, "Unable to start chat right now.", "bot");
        });
      } else {
        sendReq();
      }
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

    form.addEventListener("submit", function (e) {
      e.preventDefault();
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

