// Auto-dismiss alerts after 4s
document.querySelectorAll('.alert-custom').forEach(el => {
  setTimeout(() => el.style.opacity = '0', 4000);
  setTimeout(() => el.remove(), 4300);
});

// Risk bar colors
function getRiskColor(score) {
  if (score >= 80) return '#ef4444';
  if (score >= 60) return '#f97316';
  if (score >= 35) return '#f59e0b';
  return '#10b981';
}

document.querySelectorAll('.risk-bar-fill[data-score]').forEach(el => {
  const score = parseInt(el.dataset.score, 10);
  el.style.width = score + '%';
  el.style.background = getRiskColor(score);
});

document.querySelectorAll('.risk-bar-score[data-score]').forEach(el => {
  const score = parseInt(el.dataset.score, 10);
  el.style.color = getRiskColor(score);
});

// ── Dashboard charts ──
const riskCtx = document.getElementById('riskChart');
if (riskCtx && window._riskData) {
  const data = window._riskData;
  new Chart(riskCtx, {
    type: 'doughnut',
    data: {
      labels: data.labels,
      datasets: [{
        data: data.values,
        backgroundColor: ['#10b981','#f59e0b','#ef4444','#ff3b3b'],
        borderColor: '#141b2d',
        borderWidth: 3,
        hoverOffset: 8
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '72%',
      plugins: {
        legend: { position: 'right', labels: { color: '#8892a4', boxWidth: 12, padding: 16, font: { size: 11 } } },
        tooltip: { callbacks: { label: ctx => ` ${ctx.label}: ${ctx.parsed}` } }
      }
    }
  });
}

const trendCtx = document.getElementById('trendChart');
if (trendCtx && window._trendData) {
  const data = window._trendData;
  new Chart(trendCtx, {
    type: 'line',
    data: {
      labels: data.labels,
      datasets: [
        {
          label: 'High Risk',
          data: data.highRisk,
          borderColor: '#ef4444',
          backgroundColor: 'rgba(239,68,68,0.08)',
          fill: true,
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: '#ef4444'
        },
        {
          label: 'Approved',
          data: data.approved,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16,185,129,0.08)',
          fill: true,
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: '#10b981'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { labels: { color: '#8892a4', boxWidth: 12, font: { size: 11 } } }
      },
      scales: {
        x: { ticks: { color: '#4a5568', font: { size: 10 } }, grid: { color: '#1e2d45' } },
        y: { ticks: { color: '#4a5568', font: { size: 10 } }, grid: { color: '#1e2d45' }, beginAtZero: true }
      }
    }
  });
}

// ── AI Chat ──
const chatMessages = document.getElementById('chatMessages');
const chatInput = document.getElementById('chatInput');
const chatForm = document.getElementById('chatForm');

if (chatForm) {
  chatForm.addEventListener('submit', async e => {
    e.preventDefault();
    const msg = chatInput.value.trim();
    if (!msg) return;
    appendMessage('You', msg, 'user');
    chatInput.value = '';
    const thinkingEl = appendMessage('FraudGuard AI', '...', 'ai');
    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: msg })
      });
      const data = await res.json();
      thinkingEl.querySelector('.message-body').textContent = data.response || data.error;
    } catch {
      thinkingEl.querySelector('.message-body').textContent = 'Connection error. Please try again.';
    }
    chatMessages.scrollTop = chatMessages.scrollHeight;
  });
}

function appendMessage(sender, text, type) {
  const div = document.createElement('div');
  div.className = `message-${type}`;
  div.innerHTML = `<div class="message-sender">${sender}</div><div class="message-body">${text}</div>`;
  chatMessages.appendChild(div);
  chatMessages.scrollTop = chatMessages.scrollHeight;
  return div;
}

function useSuggestion(text) {
  if (chatInput) { chatInput.value = text; chatInput.focus(); }
}
