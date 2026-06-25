(function () {
  'use strict';

  var BLOCK_HTML = [
    '<div style="position:fixed;inset:0;background:#000;z-index:99999;display:flex;',
    'flex-direction:column;align-items:center;justify-content:center;',
    'font-family:\'Courier New\',monospace;text-align:center;padding:20px;gap:16px">',
    '<link rel="stylesheet" href="/static/css/fa.all.min.css">',
    '<i class="fa-solid fa-ban" style="font-size:5rem;color:#e50914"></i>',
    '<div style="font-size:1.6rem;font-weight:bold;color:#e50914;letter-spacing:4px">NOT FOUND</div>',
    '<div style="color:#fff;font-size:1rem;letter-spacing:2px">JANGAN CURI, MENCOBA DEBUGGING</div>',
    '<div style="color:#444;font-size:.8rem;letter-spacing:3px">SUPPORT BY KICEN XENSAI</div>',
    '</div>'
  ].join('');

  function block() {
    document.documentElement.innerHTML = BLOCK_HTML;
  }

  document.addEventListener('contextmenu', function (e) { e.preventDefault(); });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'F12') { e.preventDefault(); block(); return; }
    if (e.ctrlKey && e.shiftKey && ['I', 'J', 'C'].includes(e.key)) { e.preventDefault(); block(); return; }
    if (e.ctrlKey && e.key === 'U') { e.preventDefault(); block(); return; }
    if (e.ctrlKey && e.key === 'S') { e.preventDefault(); return; }
  });

})();
