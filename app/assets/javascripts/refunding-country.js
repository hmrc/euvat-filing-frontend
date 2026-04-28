// Copy accessible-autocomplete typed input into hidden field `valueTyped` before submit
(function () {
  function init() {
    var form = document.querySelector('form');
    if (!form) return;
    form.addEventListener('submit', function () {
      var auto = form.querySelector('.accessible-autocomplete__input, .autocomplete__input, input[role="combobox"]');
      var hidden = document.getElementById('valueTyped');
      if (auto && hidden) hidden.value = auto.value || '';
    }, false);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
