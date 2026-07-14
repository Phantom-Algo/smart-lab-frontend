mergeInto(LibraryManager.library, {
  SmartLabBridge_PostMessage: function (jsonPtr) {
    try {
      var rawJson = UTF8ToString(jsonPtr);

      if (window.SmartLabUnityBridge && typeof window.SmartLabUnityBridge.emitRaw === 'function') {
        window.SmartLabUnityBridge.emitRaw(rawJson);
        return;
      }

      var parsed = JSON.parse(rawJson);
      if (window.parent && window.parent !== window) {
        window.parent.postMessage({ type: 'unity-event', payload: parsed }, window.location.origin);
      }
    } catch (error) {
      console.error('[SmartLabBridge] Unity -> Web 消息转发失败', error);
    }
  },

  // ────────────────────────────────────────────
  //  IME 中文输入法支持
  // ────────────────────────────────────────────

  SmartLabIME_Create: function () {
    if (window.__smartLabIMEInput) return;

    var input = document.createElement('textarea');
    input.id = '__smartlab_ime_input';
    input.style.cssText = 'position:fixed;z-index:99999;border:2px solid #4A90D9;border-radius:6px;padding:8px 12px;font-size:16px;font-family:sans-serif;outline:none;background:rgba(20,25,35,0.96);color:#e8eaf0;left:-999px;top:-999px;pointer-events:auto;box-shadow:0 4px 16px rgba(0,0,0,0.5);resize:none;overflow:hidden;line-height:20px;';
    input.setAttribute('autocomplete', 'off');
    document.body.appendChild(input);

    window.__smartLabIMEInput = input;
    window.__smartLabIMETarget = '';
    window.__smartLabIMEFieldName = '';
    window.__smartLabIMEText = '';
    window.__smartLabIMEFinalized = false;
    window.__smartLabIMEComposing = false;

    // 阻止键盘事件冒泡到 Unity canvas（防止空格/Esc 误操作）
    var isVisible = function () {
      return window.__smartLabIMEInput && window.__smartLabIMEInput.style.left !== '-999px';
    };
    var blockUnityEvent = function (e) {
      if (isVisible()) {
        e.stopImmediatePropagation();
      }
    };
    var blockKeys = function (e) {
      if (window.__smartLabIMEInput && window.__smartLabIMEInput.style.left !== '-999px') {
        if (e.target === window.__smartLabIMEInput) {
          return;
        }
        e.stopImmediatePropagation();
      }
    };
    document.addEventListener('keydown', blockKeys, true);
    document.addEventListener('keyup', blockKeys, true);
    document.addEventListener('keypress', blockKeys, true);
    input.addEventListener('mousedown', blockUnityEvent, true);
    input.addEventListener('mouseup', blockUnityEvent, true);
    input.addEventListener('click', blockUnityEvent, true);
    input.addEventListener('touchstart', blockUnityEvent, true);

    input.addEventListener('compositionstart', function () {
      window.__smartLabIMEComposing = true;
    });

    input.addEventListener('compositionend', function () {
      window.__smartLabIMEComposing = false;
      window.__smartLabIMEText = input.value;
    });

    input.addEventListener('input', function () {
      window.__smartLabIMEText = input.value;
    });

    var finalize = function () {
      var v = input.value;
      window.__smartLabIMEText = v;
      window.__smartLabIMEFinalized = true;
      input.style.left = '-999px';
      input.style.top = '-999px';
    };

    input.addEventListener('blur', function () {
      setTimeout(finalize, 120);
    });

    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.keyCode === 13) {
        if (window.__smartLabIMEComposing || e.isComposing) {
          return;
        }
        e.preventDefault();
        finalize();
        input.blur();
      }
    });
  },

  SmartLabIME_Show: function (x, y, w, h, textPtr, targetPtr, fieldNamePtr) {
    if (!window.__smartLabIMEInput) {
      _SmartLabIME_Create();
    }
    var input = window.__smartLabIMEInput;
    var text = textPtr ? UTF8ToString(textPtr) : '';
    var target = targetPtr ? UTF8ToString(targetPtr) : 'IMEHandler';
    var fieldName = fieldNamePtr ? UTF8ToString(fieldNamePtr) : '';

    window.__smartLabIMETarget = target;
    window.__smartLabIMEFieldName = fieldName;
    window.__smartLabIMEText = '';
    window.__smartLabIMEFinalized = false;
    window.__smartLabIMEComposing = false;

    var canvas = document.querySelector('canvas');
    var canvasOffsetX = 0;
    var canvasOffsetY = 0;
    var scaleX = 1;
    var scaleY = 1;
    if (canvas) {
      var rect = canvas.getBoundingClientRect();
      canvasOffsetX = rect.left;
      canvasOffsetY = rect.top;
      var internalWidth = canvas.width || rect.width || 1;
      var internalHeight = canvas.height || rect.height || 1;
      scaleX = rect.width / internalWidth;
      scaleY = rect.height / internalHeight;
    }

    input.value = text;
    input.style.left = (canvasOffsetX + x * scaleX) + 'px';
    input.style.top = (canvasOffsetY + y * scaleY) + 'px';
    input.style.width = Math.max(w * scaleX, 120) + 'px';
    input.style.height = Math.max(h * scaleY, 32) + 'px';
    input.style.display = 'block';
    input.focus();
    var immediateEnd = input.value.length;
    if (typeof input.setSelectionRange === 'function') {
      input.setSelectionRange(immediateEnd, immediateEnd);
    }
    setTimeout(function () {
      input.focus();
      var end = input.value.length;
      if (typeof input.setSelectionRange === 'function') {
        input.setSelectionRange(end, end);
      }
    }, 50);
  },

  SmartLabIME_Hide: function () {
    if (window.__smartLabIMEInput) {
      window.__smartLabIMEInput.style.left = '-999px';
      window.__smartLabIMEInput.style.top = '-999px';
    }
  },

  SmartLabIME_IsVisible: function () {
    if (window.__smartLabIMEInput) {
      return window.__smartLabIMEInput.style.left !== '-999px' ? 1 : 0;
    }
    return 0;
  },

  SmartLabIME_HasText: function () {
    return (window.__smartLabIMEFinalized === true) ? 1 : 0;
  },

  SmartLabIME_GetText: function () {
    window.__smartLabIMEFinalized = false;
    var text = window.__smartLabIMEText || '';
    var fieldName = window.__smartLabIMEFieldName || '';
    var result = text + '|' + fieldName;

    var bufferSize = lengthBytesUTF8(result) + 1;
    var buffer = _malloc(bufferSize);
    stringToUTF8(result, buffer, bufferSize);
    return buffer;
  }
});
