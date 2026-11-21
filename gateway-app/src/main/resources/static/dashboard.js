/**
 * MAG Demo Dashboard — JavaScript
 * =================================
 * Interacts with the Admin REST API (/admin/metrics, /admin/routes)
 * and the Gateway Servlet entry point (/api/**).
 *
 * Security:
 *   - Admin credentials are typed into the UI only — NEVER stored in code or persisted (MAG-QR-07).
 */

const API_BASE = '';

function getAdminToken() {
    const input = document.getElementById('admin-token');
    return input ? input.value.trim() : '';
}

function authHeaders() {
    const token = getAdminToken();
    const headers = { 'Content-Type': 'application/json' };
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : 'Bearer ' + token;
    }
    return headers;
}

// =====================================================================
// Panel 1: Live Metrics
// =====================================================================

async function refreshMetrics() {
    try {
        const response = await fetch('/admin/metrics', {
            method: 'GET',
            headers: authHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            document.getElementById('metric-allowed').textContent = '—';
            document.getElementById('metric-blocked').textContent = '—';
            document.getElementById('metric-cache-ratio').textContent = '—';
            document.getElementById('metric-circuit-state').textContent = 'UNAUTHORIZED';
            document.getElementById('metric-circuit-state').style.color = 'var(--accent-rose)';
            return;
        }

        if (!response.ok) {
            throw new Error(`Metrics HTTP ${response.status}`);
        }

        const data = await response.json();
        document.getElementById('metric-allowed').textContent = data.allowedRequests ?? 0;
        document.getElementById('metric-blocked').textContent = data.blockedRequests ?? 0;
        document.getElementById('metric-cache-ratio').textContent = data.cacheHitRatio ?? '0.0%';

        const circuitElem = document.getElementById('metric-circuit-state');
        const state = data.circuitState ?? 'CLOSED';
        circuitElem.textContent = state;

        if (state === 'CLOSED') {
            circuitElem.style.color = 'var(--accent-emerald)';
        } else if (state === 'OPEN') {
            circuitElem.style.color = 'var(--accent-rose)';
        } else {
            circuitElem.style.color = 'var(--accent-amber)';
        }
    } catch (err) {
        console.error('Failed to fetch metrics:', err);
        document.getElementById('metric-circuit-state').textContent = 'ERROR';
        document.getElementById('metric-circuit-state').style.color = 'var(--accent-rose)';
    }
}

document.getElementById('btn-refresh-metrics').addEventListener('click', refreshMetrics);

// =====================================================================
// Panel 2: Routes & Rules Viewer (with inline limit update)
// =====================================================================

async function loadRoutes() {
    const container = document.getElementById('routes-list');
    try {
        const response = await fetch('/admin/routes', {
            method: 'GET',
            headers: authHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            container.innerHTML = `
                <div style="color: var(--accent-rose); padding: 0.5rem 0;">
                    <strong>[Access Restricted]</strong> Admin credential required to view active routes.
                    <br><small style="color: var(--text-muted);">Enter the admin token in the header bar above and click "Refresh Metrics".</small>
                </div>`;
            return;
        }

        if (!response.ok) {
            container.innerHTML = `<span style="color: var(--accent-rose)">Failed to load routes (HTTP ${response.status})</span>`;
            return;
        }

        const routes = await response.json();
        if (!Array.isArray(routes) || routes.length === 0) {
            container.innerHTML = '<span style="color: var(--text-muted)">No active routes registered in snapshot.</span>';
            return;
        }

        let html = `
            <div style="overflow-x: auto;">
                <table style="width: 100%; border-collapse: collapse; font-size: 0.8rem; text-align: left;">
                    <thead>
                        <tr style="border-bottom: 1px solid var(--border); color: var(--text-secondary);">
                            <th style="padding: 0.5rem;">Route ID</th>
                            <th style="padding: 0.5rem;">Pattern</th>
                            <th style="padding: 0.5rem;">Target Upstream</th>
                            <th style="padding: 0.5rem;">Cache</th>
                            <th style="padding: 0.5rem;">TTL (s)</th>
                            <th style="padding: 0.5rem;">Auth</th>
                            <th style="padding: 0.5rem;">Action</th>
                        </tr>
                    </thead>
                    <tbody>`;

        routes.forEach(route => {
            const cacheBadge = route.cacheEnabled
                ? `<span style="color: var(--accent-emerald); font-weight:600;">ENABLED</span>`
                : `<span style="color: var(--text-muted);">DISABLED</span>`;
            const authBadge = route.authRequired
                ? `<span style="color: var(--accent-amber); font-weight:600;">REQUIRED</span>`
                : `<span style="color: var(--text-muted);">OPTIONAL</span>`;
            const upstreams = (route.upstreamUrls || []).join(', ');

            html += `
                <tr style="border-bottom: 1px solid rgba(255,255,255,0.04);">
                    <td style="padding: 0.5rem; font-weight:600; color: var(--accent-cyan);">${escapeHtml(route.id)}</td>
                    <td style="padding: 0.5rem;"><code>${escapeHtml(route.pathPattern)}</code></td>
                    <td style="padding: 0.5rem; color: var(--text-secondary);">${escapeHtml(upstreams)}</td>
                    <td style="padding: 0.5rem;">${cacheBadge}</td>
                    <td style="padding: 0.5rem;">
                        <input type="number" id="ttl-${escapeHtml(route.id)}" value="${route.cacheTtlSeconds ?? 0}" 
                               style="width: 60px; background: var(--bg-input); border: 1px solid var(--border); color: #fff; padding: 0.2rem 0.4rem; border-radius: 4px; font-size: 0.75rem;" />
                    </td>
                    <td style="padding: 0.5rem;">${authBadge}</td>
                    <td style="padding: 0.5rem;">
                        <button onclick="updateRouteTtl('${escapeHtml(route.id)}')" 
                                style="background: var(--accent-blue); color: #fff; border:none; padding: 0.2rem 0.6rem; border-radius: 4px; font-size: 0.75rem; cursor: pointer;">
                            Update
                        </button>
                    </td>
                </tr>`;
        });

        html += `</tbody></table></div>`;
        container.innerHTML = html;
    } catch (err) {
        console.error('Failed to load routes:', err);
        container.innerHTML = `<span style="color: var(--accent-rose)">Error connecting to Admin API: ${escapeHtml(err.message)}</span>`;
    }
}

async function updateRouteTtl(routeId) {
    const input = document.getElementById(`ttl-${routeId}`);
    if (!input) return;
    const newTtl = parseInt(input.value, 10);
    if (isNaN(newTtl) || newTtl < 0) {
        alert('Please enter a valid non-negative TTL value.');
        return;
    }

    try {
        const response = await fetch(`/admin/routes/${encodeURIComponent(routeId)}`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify({ cacheTtlSeconds: newTtl, cacheEnabled: newTtl > 0 })
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            alert(`Failed to update route: ${errData.error || response.statusText}`);
            return;
        }

        alert(`Route ${routeId} updated successfully (Cache TTL: ${newTtl}s)!`);
        await loadRoutes();
    } catch (err) {
        alert(`Update error: ${err.message}`);
    }
}

// Expose updateRouteTtl globally for onclick handlers
window.updateRouteTtl = updateRouteTtl;

// =====================================================================
// Panel 3: Request Tester (with typed JWT, status, headers & response)
// =====================================================================

document.getElementById('btn-send-request').addEventListener('click', async () => {
    const method = document.getElementById('tester-method').value;
    const path   = document.getElementById('tester-path').value.trim();
    const token  = document.getElementById('tester-token').value.trim();
    const result = document.getElementById('tester-result');

    result.textContent = 'Dispatching request through gateway...';

    const headers = {};
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : 'Bearer ' + token;
    }

    const startTime = performance.now();
    try {
        const response = await fetch(path, {
            method: method,
            headers: headers
        });
        const elapsedMs = Math.round(performance.now() - startTime);

        const responseText = await response.text();
        let formattedBody = responseText;
        try {
            const json = JSON.parse(responseText);
            formattedBody = JSON.stringify(json, null, 2);
        } catch (_) {
            // Not JSON; keep raw string
        }

        // Collect response headers
        const headerLines = [];
        response.headers.forEach((val, key) => {
            headerLines.push(`${key}: ${val}`);
        });

        const statusColor = response.ok ? '#10b981' : (response.status === 503 ? '#f59e0b' : '#f43f5e');

        result.innerHTML = `
<span style="color: ${statusColor}; font-weight: bold;">HTTP/1.1 ${response.status} ${response.statusText}</span>  <span style="color: var(--text-muted); font-size: 0.75rem;">(${elapsedMs} ms)</span>

<span style="color: var(--text-secondary); font-weight: 600;">[Headers]</span>
${escapeHtml(headerLines.join('\n')) || '(none)'}

<span style="color: var(--text-secondary); font-weight: 600;">[Body]</span>
${escapeHtml(formattedBody)}`;

        // Refresh metrics after request
        await refreshMetrics();
    } catch (err) {
        result.textContent = `Gateway dispatch error: ${err.message}\nMake sure the gateway is running on http://localhost:8080`;
    }
});

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Initial bootstrap
const btnApply = document.getElementById('btn-apply-token');
if (btnApply) {
    btnApply.addEventListener('click', () => {
        refreshMetrics();
        loadRoutes();
    });
}

document.getElementById('admin-token').addEventListener('change', () => {
    refreshMetrics();
    loadRoutes();
});

// Set default sample admin token in placeholder guide
refreshMetrics();
loadRoutes();
