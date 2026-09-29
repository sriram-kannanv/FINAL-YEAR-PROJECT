/**
 * IoT Baseline Dashboard — Client-Side Logic
 * Conventional IoT-Cloud Existing System (Review 2)
 *
 * Polls the REST API every 5 seconds to keep the dashboard
 * displaying live data from the backend.
 */

'use strict';

// -------------------------------------------------------
// Chart instances
// -------------------------------------------------------
let tempChart = null;
let humidityChart = null;

// -------------------------------------------------------
// Pagination state
// -------------------------------------------------------
let currentPage = 0;
const PAGE_SIZE = 15;

// -------------------------------------------------------
// Polling
// -------------------------------------------------------
const POLL_INTERVAL_MS = 5000;

// -------------------------------------------------------
// Init
// -------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    initCharts();
    loadAll();
    startClock();
    setInterval(loadAll, POLL_INTERVAL_MS);
    populateDeviceSelects();
    setInterval(populateDeviceSelects, 15000);
});

function loadAll() {
    loadStats();
    loadLatestReading();
    loadSimulatorStatus();
    loadSystemStatus();
    loadChartData();
    loadReadings();
    loadDevices();
}

// -------------------------------------------------------
// Clock
// -------------------------------------------------------
function startClock() {
    function tick() {
        const el = document.getElementById('hdr-time');
        if (el) el.textContent = new Date().toLocaleString('en-IN', { hour12: false });
    }
    tick();
    setInterval(tick, 1000);
}

// -------------------------------------------------------
// KPI Stats
// -------------------------------------------------------
async function loadStats() {
    try {
        const [stats, sensorStats] = await Promise.all([
            fetchJSON('/api/monitoring/stats'),
            fetchJSON('/api/sensors/stats')
        ]);

        setText('kpi-total', stats.totalReadings ?? '—');
        setText('kpi-rejected', stats.rejectedMessages ?? '—');
        setText('kpi-devices', stats.activeDevices ?? '—');
        setText('kpi-total-devices', stats.totalDevices ?? '?');

        // MQTT badge
        const mqttBadge = document.getElementById('hdr-mqtt-badge');
        if (mqttBadge) {
            const connected = stats.mqttConnected;
            mqttBadge.textContent = connected ? 'MQTT: Connected' : 'MQTT: Disconnected';
            mqttBadge.className = 'status-badge ' + (connected ? 'badge-online' : 'badge-offline');
        }
    } catch (e) {
        console.warn('Stats load error:', e);
    }
}

// -------------------------------------------------------
// Latest Reading (KPI temp/humidity)
// -------------------------------------------------------
async function loadLatestReading() {
    try {
        const data = await fetchJSON('/api/sensors/readings/latest');
        if (data && data.temperature !== undefined) {
            setText('kpi-temp', data.temperature.toFixed(1) + '°C');
            setText('kpi-humidity', data.humidity.toFixed(1) + '%');
            const ts = data.timestamp ? new Date(data.timestamp).toLocaleTimeString('en-IN') : '—';
            setText('kpi-temp-sub', 'DHT22 · Last at ' + ts);
            setText('kpi-humidity-sub', 'DHT22 · Last at ' + ts);
        }
    } catch (e) {
        console.warn('Latest reading error:', e);
    }
}

// -------------------------------------------------------
// Charts
// -------------------------------------------------------
function initCharts() {
    const chartDefaults = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: false },
            tooltip: {
                backgroundColor: 'rgba(22,27,34,0.95)',
                borderColor: 'rgba(48,54,61,0.8)',
                borderWidth: 1,
                titleColor: '#e6edf3',
                bodyColor: '#8b949e',
                cornerRadius: 8,
                padding: 10
            }
        },
        scales: {
            x: {
                ticks: { color: '#484f58', font: { size: 10 }, maxTicksLimit: 8 },
                grid: { color: 'rgba(48,54,61,0.3)' }
            },
            y: {
                ticks: { color: '#484f58', font: { size: 10 } },
                grid: { color: 'rgba(48,54,61,0.3)' }
            }
        },
        elements: {
            point: { radius: 3, hoverRadius: 5, borderWidth: 2 },
            line: { tension: 0.4, borderWidth: 2, fill: true }
        }
    };

    const tempCtx = document.getElementById('temp-chart').getContext('2d');
    tempChart = new Chart(tempCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Temperature (°C)',
                data: [],
                borderColor: '#f59e0b',
                backgroundColor: 'rgba(245,158,11,0.08)',
                pointBackgroundColor: '#f59e0b',
                pointBorderColor: '#f59e0b'
            }]
        },
        options: {
            ...chartDefaults,
            scales: {
                ...chartDefaults.scales,
                y: { ...chartDefaults.scales.y, title: { display: true, text: '°C', color: '#484f58' } }
            }
        }
    });

    const humCtx = document.getElementById('humidity-chart').getContext('2d');
    humidityChart = new Chart(humCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Humidity (%)',
                data: [],
                borderColor: '#06b6d4',
                backgroundColor: 'rgba(6,182,212,0.08)',
                pointBackgroundColor: '#06b6d4',
                pointBorderColor: '#06b6d4'
            }]
        },
        options: {
            ...chartDefaults,
            scales: {
                ...chartDefaults.scales,
                y: {
                    ...chartDefaults.scales.y,
                    min: 0, max: 100,
                    title: { display: true, text: '%', color: '#484f58' }
                }
            }
        }
    });
}

async function loadChartData() {
    try {
        const hours   = document.getElementById('chart-hours-select')?.value || 24;
        const deviceId = document.getElementById('chart-device-select')?.value || '';
        let url = `/api/sensors/readings/chart?hours=${hours}`;
        if (deviceId) url += `&deviceId=${encodeURIComponent(deviceId)}`;

        const readings = await fetchJSON(url);

        if (!Array.isArray(readings) || readings.length === 0) {
            clearCharts();
            return;
        }

        const sorted = readings.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
        const labels = sorted.map(r => fmtTime(r.timestamp));
        const temps  = sorted.map(r => r.temperature);
        const hums   = sorted.map(r => r.humidity);

        updateChart(tempChart, labels, temps);
        updateChart(humidityChart, labels, hums);
    } catch (e) {
        console.warn('Chart load error:', e);
    }
}

function clearCharts() {
    if (tempChart)     { tempChart.data.labels = [];     tempChart.data.datasets[0].data = [];     tempChart.update(); }
    if (humidityChart) { humidityChart.data.labels = []; humidityChart.data.datasets[0].data = []; humidityChart.update(); }
}

function updateChart(chart, labels, data) {
    chart.data.labels = labels;
    chart.data.datasets[0].data = data;
    chart.update('none');
}

document.getElementById('chart-hours-select')?.addEventListener('change', loadChartData);
document.getElementById('chart-device-select')?.addEventListener('change', loadChartData);

// -------------------------------------------------------
// Readings Table
// -------------------------------------------------------
async function loadReadings() {
    const deviceId = document.getElementById('filter-device')?.value || '';
    let url = `/api/sensors/readings?page=${currentPage}&size=${PAGE_SIZE}`;
    if (deviceId) url += `&deviceId=${encodeURIComponent(deviceId)}`;

    try {
        const data = await fetchJSON(url);
        renderReadingsTable(data.readings || []);
        const count = data.totalElements || 0;
        const pages = data.totalPages || 1;
        setText('readings-count', `Showing page ${currentPage + 1} of ${pages} (${count} total readings)`);
        document.getElementById('prev-page').disabled = currentPage === 0;
        document.getElementById('next-page').disabled = currentPage >= pages - 1;
    } catch (e) {
        console.warn('Readings load error:', e);
    }
}

function renderReadingsTable(readings) {
    const tbody = document.getElementById('readings-tbody');
    if (!tbody) return;

    if (!readings.length) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);padding:2rem">No readings yet — start the simulator to see data</td></tr>';
        return;
    }

    tbody.innerHTML = readings.map((r, i) => `
        <tr>
            <td class="text-muted">${r.id || i + 1}</td>
            <td><code style="color:var(--accent-cyan);font-size:0.8em">${esc(r.deviceId || '—')}</code></td>
            <td class="val-temp">${r.quality === 'VALID' ? r.temperature.toFixed(1) + ' °C' : '—'}</td>
            <td class="val-hum">${r.quality === 'VALID' ? r.humidity.toFixed(1) + ' %' : '—'}</td>
            <td class="text-mono" style="font-size:0.78em">${fmtTs(r.timestamp)}</td>
            <td>${r.simulated ? '<span style="color:var(--accent-amber);font-size:0.75em">⚡ Simulated</span>' : '<span style="color:var(--accent-green);font-size:0.75em">📡 Device</span>'}</td>
            <td>${qualityBadge(r.quality)}</td>
            <td class="text-mono text-muted" style="font-size:0.72em">${esc(r.mqttTopic || '—')}</td>
        </tr>
    `).join('');
}

function qualityBadge(q) {
    const map = {
        'VALID': '<span style="color:var(--accent-green);font-size:0.75em">✓ Valid</span>',
        'AUTH_FAILED': '<span style="color:var(--accent-red);font-size:0.75em">✗ Auth Fail</span>',
        'INVALID_RANGE': '<span style="color:var(--accent-amber);font-size:0.75em">⚠ Range</span>'
    };
    return map[q] || q;
}

function changePage(delta) {
    currentPage = Math.max(0, currentPage + delta);
    loadReadings();
}

document.getElementById('filter-device')?.addEventListener('change', () => {
    currentPage = 0;
    loadReadings();
});

// -------------------------------------------------------
// Devices List
// -------------------------------------------------------
async function loadDevices() {
    try {
        const devices = await fetchJSON('/api/devices');
        const container = document.getElementById('devices-list');
        if (!container) return;

        if (!devices.length) {
            container.innerHTML = '<div style="color:var(--text-muted);text-align:center;padding:1rem">No devices registered yet.</div>';
            return;
        }

        container.innerHTML = devices.map(d => `
            <div class="status-row">
                <div>
                    <div class="status-row-label gap-icon">
                        ${statusDot(d.status)}
                        <span>${esc(d.deviceName || d.deviceId)}</span>
                    </div>
                    <div style="font-size:0.72em;color:var(--text-muted);margin-top:0.2rem;margin-left:1.25rem">
                        ID: <code style="color:var(--accent-cyan)">${esc(d.deviceId)}</code> &bull;
                        ${d.deviceType} / ${d.sensorType} &bull;
                        Readings: ${d.totalReadings ?? 0}
                    </div>
                </div>
                <div style="text-align:right">
                    ${statusBadge(d.status)}
                    <div class="text-muted" style="font-size:0.7em;margin-top:0.25rem">
                        ${d.lastSeen ? 'Last: ' + fmtTs(d.lastSeen) : 'Never connected'}
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.warn('Devices load error:', e);
    }
}

function statusDot(status) {
    const colors = { ACTIVE: '#34d399', INACTIVE: '#f87171', REGISTERED: '#fbbf24', NEVER_CONNECTED: '#484f58' };
    const c = colors[status] || '#484f58';
    return `<span style="width:8px;height:8px;background:${c};border-radius:50%;display:inline-block;flex-shrink:0;${status === 'ACTIVE' ? 'animation:pulse-dot 2s infinite' : ''}"></span>`;
}

function statusBadge(status) {
    const map = {
        'ACTIVE':          'badge-online',
        'INACTIVE':        'badge-offline',
        'REGISTERED':      'badge-warning',
        'NEVER_CONNECTED': ''
    };
    return `<span class="status-badge ${map[status] || ''}">${status}</span>`;
}

// -------------------------------------------------------
// Populate device selects
// -------------------------------------------------------
async function populateDeviceSelects() {
    try {
        const devices = await fetchJSON('/api/devices');
        const selects = ['filter-device', 'chart-device-select'];
        selects.forEach(id => {
            const sel = document.getElementById(id);
            if (!sel) return;
            const curr = sel.value;
            const options = devices.map(d => `<option value="${esc(d.deviceId)}">${esc(d.deviceId)}</option>`).join('');
            sel.innerHTML = '<option value="">All Devices</option>' + options;
            if (curr) sel.value = curr;
        });
    } catch (e) { /* ignore */ }
}

// -------------------------------------------------------
// Simulator
// -------------------------------------------------------
async function loadSimulatorStatus() {
    try {
        const status = await fetchJSON('/api/simulator/status');
        const badge = document.getElementById('sim-status-badge');
        if (badge) {
            badge.textContent = status.running ? 'Running' : 'Stopped';
            badge.className = 'status-badge ' + (status.running ? 'badge-online' : 'badge-offline');
        }
        setText('sim-published', status.publishedCount ?? 0);
        setText('sim-failed', status.failedCount ?? 0);
    } catch (e) { /* ignore */ }
}

async function startSimulator() {
    const interval = parseInt(document.getElementById('interval-slider').value, 10);
    try {
        await postJSON('/api/simulator/start', { intervalSeconds: interval });
        showToast('Simulator started (' + interval + 's interval)', 'success');
        loadSimulatorStatus();
    } catch (e) {
        showToast('Failed to start simulator', 'error');
    }
}

async function stopSimulator() {
    try {
        await postJSON('/api/simulator/stop', {});
        showToast('Simulator stopped', 'info');
        loadSimulatorStatus();
    } catch (e) {
        showToast('Failed to stop simulator', 'error');
    }
}

function updateIntervalDisplay(val) {
    const el = document.getElementById('interval-display');
    if (el) el.textContent = val + 's';
}

// -------------------------------------------------------
// Device Registration
// -------------------------------------------------------
async function registerDevice(e) {
    e.preventDefault();
    const btn    = document.getElementById('reg-submit-btn');
    const result = document.getElementById('reg-result');
    const body = {
        deviceId:   document.getElementById('reg-device-id').value.trim(),
        deviceName: document.getElementById('reg-device-name').value.trim(),
        secretKey:  document.getElementById('reg-secret').value,
        sensorType: document.getElementById('reg-sensor').value,
        deviceType: 'ESP32'
    };

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Registering…';
    result.textContent = '';

    try {
        const data = await postJSON('/api/devices/register', body);
        result.style.color = 'var(--accent-green)';
        result.textContent = '✓ ' + (data.message || 'Registered!');
        document.getElementById('reg-form').reset();
        showToast('Device registered: ' + body.deviceId, 'success');
        loadDevices();
        populateDeviceSelects();
    } catch (e) {
        result.style.color = 'var(--accent-red)';
        result.textContent = '✗ ' + (e.message || 'Registration failed');
        showToast('Registration failed', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '🔐 Register Device';
    }
}

// -------------------------------------------------------
// System Status
// -------------------------------------------------------
async function loadSystemStatus() {
    try {
        const status = await fetchJSON('/api/monitoring/status');
        setBadge('st-broker', status.brokerRunning, 'Online', 'Offline');
        setBadge('st-mqtt',   status.mqttConnected,  'Connected', 'Disconnected');
        setBadge('st-tls',    false, 'Active', 'Not Active — localhost only');
        setBadge('st-db',     true,  'Online (SQLite)', 'Error');
    } catch (e) { /* ignore */ }
}

function setBadge(id, ok, trueText, falseText) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = ok ? trueText : falseText;
    el.className = 'status-badge ' + (ok ? 'badge-online' : 'badge-offline');
}

// -------------------------------------------------------
// Utilities
// -------------------------------------------------------
async function fetchJSON(url) {
    const resp = await fetch(url, { credentials: 'same-origin' });
    if (!resp.ok) throw new Error('HTTP ' + resp.status);
    return resp.json();
}

async function postJSON(url, body) {
    const resp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(body)
    });
    const data = await resp.json();
    if (!resp.ok) throw new Error(data.error || 'Request failed');
    return data;
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function esc(str) {
    return String(str).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtTime(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
}

function fmtTs(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    return d.toLocaleString('en-IN', { hour12: false, year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function showToast(msg, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icons = { success: '✓', error: '✗', info: 'ℹ' };
    toast.innerHTML = `<span>${icons[type] || 'ℹ'}</span> ${esc(msg)}`;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}
