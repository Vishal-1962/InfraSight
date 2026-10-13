(function () {
  'use strict';

  // ---- Delete / resolve modals: fill the target URL and name from the clicked button ----
  document.addEventListener('click', function (event) {
    var del = event.target.closest('[data-delete-url]');
    if (del) {
      var form = document.getElementById('delete-form');
      if (form) {
        form.setAttribute('action', del.getAttribute('data-delete-url'));
        document.getElementById('delete-name').textContent = del.getAttribute('data-delete-name') || 'this endpoint';
      }
    }
    var res = event.target.closest('[data-resolve-url]');
    if (res) {
      var rform = document.getElementById('resolve-form');
      if (rform) {
        rform.setAttribute('action', res.getAttribute('data-resolve-url'));
        document.getElementById('resolve-name').textContent = res.getAttribute('data-resolve-name') || '';
        var note = document.getElementById('resolve-note');
        if (note) { note.value = ''; }
      }
    }
  });

  // ---- Loading state for slow actions (manual checks) ----
  document.querySelectorAll('form[data-loading]').forEach(function (form) {
    form.addEventListener('submit', function () {
      var button = form.querySelector('button[type="submit"]');
      if (!button) { return; }
      var text = form.getAttribute('data-loading');
      window.setTimeout(function () { button.disabled = true; button.textContent = text; }, 0);
    });
  });

  // ---- Charts ----
  var accent = '#2457d6';
  var grid = '#eef0f3';
  var muted = '#667085';

  function readPoints(canvas) {
    try { return JSON.parse(canvas.getAttribute('data-chart') || '[]'); } catch (e) { return []; }
  }

  function drawChart(canvas) {
    if (typeof Chart === 'undefined') { return; }
    var points = readPoints(canvas);
    var metric = canvas.getAttribute('data-metric');
    var isAvailability = metric === 'availability';
    var labels = points.map(function (p) { return p.label; });
    var values = points.map(function (p) { return isAvailability ? p.availability : p.avgResponseMs; });
    var kind = isAvailability ? 'bar' : 'line';

    Chart.defaults.font.family = '"IBM Plex Sans", -apple-system, "Segoe UI", Arial, sans-serif';
    Chart.defaults.font.size = 11;
    Chart.defaults.color = muted;

    new Chart(canvas, {
      type: kind,
      data: {
        labels: labels,
        datasets: [{
          label: isAvailability ? 'Availability (%)' : 'Avg response (ms)',
          data: values,
          borderColor: accent,
          backgroundColor: isAvailability ? 'rgba(36, 87, 214, 0.75)' : accent,
          borderWidth: isAvailability ? 0 : 1.5,
          pointRadius: 0,
          pointHoverRadius: 3,
          tension: 0.2,
          spanGaps: false,
          borderRadius: isAvailability ? 2 : 0,
          maxBarThickness: 18
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        interaction: { mode: 'index', intersect: false },
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 8 }, border: { color: grid } },
          y: {
            beginAtZero: true,
            max: isAvailability ? 100 : undefined,
            grid: { color: grid },
            border: { display: false },
            ticks: { maxTicksLimit: 5 }
          }
        }
      }
    });
  }

  document.querySelectorAll('canvas[data-chart]').forEach(drawChart);

  // ---- Auto refresh on live pages (paused while a dialog or form field is in use) ----
  if (document.body.hasAttribute('data-refresh')) {
    window.setTimeout(function () {
      var busy = document.querySelector('.modal.show') || document.querySelector('form[data-loading] button:disabled');
      var typing = document.activeElement && /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName);
      if (!busy && !typing && !document.hidden) { window.location.reload(); }
    }, 30000);
  }
})();
