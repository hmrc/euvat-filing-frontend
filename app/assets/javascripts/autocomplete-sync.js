/*
 Shared autocomplete sync helper

 Purpose
 - Centralises small but important behaviors for pages that use the
   accessible-autocomplete (or the HMRC accessible autocomplete wrapper):
   1) Ensure the server receives either a matched option value (e.g. country
      code) or the user-typed free-text.
   2) Ensure the autocomplete input and its listbox are labelled by the
      page heading for screen-reader users.

 Behaviour summary
 - Discovers autocomplete widgets (and legacy select-based widgets) on the
   page and wires a `submit` handler on the nearest `<form>`.
 - On form submit:
     * Copies the visible autocomplete input value into a hidden
       typed-value field (recommended id `#valueTyped`). This preserves
       free-text entries that don't match any option.
     * If the typed text exactly equals an option's visible text or value,
       programmatically sets the underlying `<select>` value to that option
       so the server receives the canonical option value (e.g. 'AT').
 - Patches `aria-labelledby` on the combobox and listbox so screen readers
   announce the visible heading as the label. A brief retry loop handles the
   case where the accessible-autocomplete component initialises after this
   script runs.
 - Runs a `MutationObserver` to pick up widgets added dynamically.

 Configuration (per-instance)
 - data-ac-typed (optional): CSS selector for the hidden field that should
   receive raw typed text. Example: `data-ac-typed="#valueTyped"`.
 - data-ac-heading (optional): CSS selector for the heading whose id should
   be used as `aria-labelledby`. Example: `data-ac-heading="#refunding-country-heading"`.
 - If absent, the script falls back to sensible defaults:
     * hidden typed: `#valueTyped` or `input[name="valueTyped"]`
     * heading: first `h1[id]` or `h2[id]` inside the form

 Implementation notes
 - Selectors used:
     * visible input: `.accessible-autocomplete__input`, `.autocomplete__input`,
       `input[role="combobox"]`, `input[id$="-input"]`
     * underlying select: `select[name="value"]`, `select[id$="-input"]`
 - Uses a `WeakSet` to avoid initialising the same form more than once.
 - ARIA patching uses a small `setInterval` retry (7 attempts, 100ms) because
   the HMRC component may create the listbox asynchronously. This is simple
   and robust without requiring a complex integration.
 - The script swallows DOM-related exceptions to avoid breaking pages where
   markup differs slightly; this keeps the behaviour progressive-enhancement
   friendly.

 Accessibility rationale
 - Screen readers need a clear label for the combobox and its listbox; using
   the visible heading as `aria-labelledby` provides predictable context.
 - Copying typed text to a hidden field preserves user intent when no option
   was selected; the backend can then accept or validate the free-text.

 CSP and inclusion
 - Include the script once in your page layout. In Play templates use the
   CSP nonce helper so the script plays nicely with CSP, for example:
     <script @{CSPNonce.attr} src='@controllers.routes.Assets.versioned("javascripts/autocomplete-sync.js")'></script>

 Example Scala template usage
 - In your `Select(...).attributes` map:
     "data-ac-typed" -> "#valueTyped",
     "data-ac-heading" -> "#refunding-country-heading"
 - Keep a hidden field for typed values:
     <input type="hidden" id="valueTyped" name="valueTyped" value="">

 Testing tips
 - Unit: use jsdom to assert `window.autocompleteSync.scan()` wires handlers.
 - Integration: Cypress can verify submit behaviour (typed text preserved,
   option matching sets the select value) and ARIA attributes are set.

 API
 - Exposes `window.autocompleteSync.scan()` and `window.autocompleteSync.syncOnSubmit(form)`
   for manual triggering in tests or edge cases.

 Small, deliberate trade-offs
 - The script prefers resilient, small-footprint approaches (retry loop,
   fallbacks) rather than strictly coupling to a specific autocomplete
   implementation; if you need stronger guarantees, add explicit
   `data-ac-*` attributes in the template.

*/
(function () {
  var AC_INPUT_SEL = '.accessible-autocomplete__input, .autocomplete__input, input[role="combobox"], input[id$="-input"]';
  var SELECT_SEL = 'select[name="value"], select[id$="-input"]';
  var initialized = new WeakSet();

  function toArray(nl) { return Array.prototype.slice.call(nl || []); }

  function findHeadingId(form, select) {
    // priority: data attribute on select -> explicit heading in form
    if (select) {
      var d = select.getAttribute('data-ac-heading');
      if (d) return d.replace(/^#/, '');
    }
    var h = form.querySelector('h1[id], h2[id]');
    return h ? h.id : null;
  }

  function findHiddenTyped(form, select) {
    if (select) {
      var d = select.getAttribute('data-ac-typed');
      if (d) {
        try { return document.querySelector(d); } catch (e) { /* ignore */ }
      }
    }
    return form.querySelector('#valueTyped, input[name="valueTyped"]');
  }

  function patchAria(acInput, headingId) {
    if (!acInput || !headingId) return;
    var tries = 0;
    var iv = setInterval(function () {
      try {
        var listbox = document.querySelector('[role="listbox"][id$="__listbox"]');
        acInput.setAttribute('aria-labelledby', headingId);
        if (listbox) listbox.setAttribute('aria-labelledby', headingId);
      } catch (e) {
        // ignore
      }
      if (++tries > 6) clearInterval(iv);
    }, 100);
  }

  function syncOnSubmit(form) {
    if (initialized.has(form)) return;
    initialized.add(form);

    form.addEventListener('submit', function () {
      var acInput = form.querySelector(AC_INPUT_SEL);
      var select = form.querySelector(SELECT_SEL);
      var hidden = findHiddenTyped(form, select);

      if (acInput && hidden) hidden.value = acInput.value || '';

      try {
        if (select && acInput && acInput.value) {
          var v = (acInput.value || '').trim();
          if (v) {
            var options = toArray(select.options || []);
            var matched = options.find(function (opt) {
              return ((opt.text || '').trim() === v) || ((opt.value || '').trim() === v);
            });
            if (matched) select.value = matched.value;
          }
        }
      } catch (e) {
        // fail silently
      }
    }, false);

    // patch ARIA after widget initialises (supports async initialisation)
    var acInput = form.querySelector(AC_INPUT_SEL);
    var select = form.querySelector(SELECT_SEL);
    var headingId = findHeadingId(form, select);
    // If the underlying <select> has specified input classes (e.g. from the
    // Select view model), apply them to the generated autocomplete input so
    // it can pick up GOV.UK width utilities like `govuk-!-width-two-thirds`.
    try {
      if (acInput && select) {
        var acClasses = select.getAttribute('data-ac-input-classes') || select.getAttribute('data-ac-input-class');
        if (acClasses) {
          acClasses.split(/\s+/).forEach(function (c) { if (c) acInput.classList.add(c); });
        }
      }
    } catch (e) { /* ignore DOM exceptions */ }
    if (acInput && headingId) patchAria(acInput, headingId);
    // Ensure any requested input classes persist across component re-renders
    // (accessible-autocomplete may replace the input's `class` on focus).
    try {
      if (acInput && select) {
        var acClassesAttr = select.getAttribute('data-ac-input-classes') || select.getAttribute('data-ac-input-class');
        if (acClassesAttr) {
            var acClassesArr = acClassesAttr.split(/\s+/).filter(Boolean);
            var ensureClasses = function () {
              acClassesArr.forEach(function (c) { if (c && !acInput.classList.contains(c)) acInput.classList.add(c); });
            };

            // Utility to size the listbox to match the input width
            var adjustMenuWidth = function () {
              try {
                var listbox = document.querySelector('[role="listbox"][id$="__listbox"]');
                if (!listbox) return;
                var rect = acInput.getBoundingClientRect();
                listbox.style.width = rect.width + 'px';
              } catch (e) { /* ignore */ }
            };

            // initial ensure and sizing
            ensureClasses();
            adjustMenuWidth();

            // reapply on focus/blur events
            acInput.addEventListener && acInput.addEventListener('focus', ensureClasses);
            acInput.addEventListener && acInput.addEventListener('blur', ensureClasses);
            acInput.addEventListener && acInput.addEventListener('focus', adjustMenuWidth);
            acInput.addEventListener && acInput.addEventListener('blur', adjustMenuWidth);

            // update on window resize
            window.addEventListener && window.addEventListener('resize', adjustMenuWidth);

            // watch for class attribute changes (component re-renders)
            if (window.MutationObserver) {
              var mo = new MutationObserver(function (mutations) {
                mutations.forEach(function (m) {
                  if (m.attributeName === 'class') ensureClasses();
                  adjustMenuWidth();
                });
              });
              mo.observe(acInput, { attributes: true, attributeFilter: ['class'] });
            }
        }
      }
    } catch (e) { /* ignore DOM exceptions */ }
  }

  function scan() {
    var forms = toArray(document.querySelectorAll('form'));
    forms.forEach(function (form) {
      if (form.querySelector(AC_INPUT_SEL) || form.querySelector(SELECT_SEL)) {
        syncOnSubmit(form);
      }
    });
  }

  // observe for dynamically added autocomplete nodes
  function observe() {
    if (!window.MutationObserver) return;
    var obs = new MutationObserver(function () { scan(); });
    obs.observe(document.documentElement || document.body, { childList: true, subtree: true });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { scan(); observe(); });
  } else {
    scan(); observe();
  }

  // expose small API for tests or edge cases
  window.autocompleteSync = { scan: scan, syncOnSubmit: syncOnSubmit };
})();
