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
