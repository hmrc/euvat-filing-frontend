// Sync accessible-autocomplete visible input back into the underlying select
// and copy the typed value into hidden `valueTyped` before submit.
(function () {
  function findSelect(form) {
    return form.querySelector('select[name="value"], select[id$="-input"]');
  }

  function init() {
    var form = document.querySelector('form');
    if (!form) return;

    form.addEventListener('submit', function () {
      var auto = form.querySelector('.accessible-autocomplete__input, .autocomplete__input, input[role="combobox"]');
      var hidden = document.getElementById('valueTyped');
      var select = findSelect(form);

      if (auto && hidden) hidden.value = auto.value || '';

      // If an underlying select exists, try to find an option whose visible
      // text matches the autocomplete value and set the select value to the
      // corresponding option value (e.g. use 'Austria' -> 'AT'). This ensures
      // the form posts the code expected by server validation.
      try {
        if (select && auto && auto.value) {
          var v = auto.value.trim();
          var options = Array.prototype.slice.call(select.options || []);
          var matched = options.find(function (opt) {
            return (opt.text || '').trim() === v || (opt.value || '').trim() === v;
          });
          if (matched) {
            select.value = matched.value;
          }
        }
      } catch (e) {
        // fail silently
      }
    }, false);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

// We rely on HMRC accessible-autocomplete initialisation for accessible
// attributes. Keep a small submit-sync helper only: copy visible input into
// hidden `#valueTyped` and set the underlying select value when a matching
// option exists.
// (The initial IIFE above already implements the needed submit handler.)

// Small runtime patch: ensure the autocomplete input and listbox use the
// visible page heading as their `aria-labelledby` source. Retry briefly to
// catch the HMRC component initialising after our script.
(function () {
  function patchLabelledby() {
    try {
      var heading = document.getElementById('refunding-country-heading');
      if (!heading) return;
      var acInput = document.querySelector('.accessible-autocomplete__wrapper input, .autocomplete__input, input[role="combobox"], input[id$="-input"]');
      var listbox = document.querySelector('[role="listbox"][id$="__listbox"]');
      if (acInput) acInput.setAttribute('aria-labelledby', 'refunding-country-heading');
      if (listbox) listbox.setAttribute('aria-labelledby', 'refunding-country-heading');
    } catch (e) {
      // silent
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      patchLabelledby();
      var tries = 0;
      var iv = setInterval(function () { patchLabelledby(); if (++tries > 6) clearInterval(iv); }, 100);
    });
  } else {
    patchLabelledby();
    var tries = 0;
    var iv = setInterval(function () { patchLabelledby(); if (++tries > 6) clearInterval(iv); }, 100);
  }
})();
