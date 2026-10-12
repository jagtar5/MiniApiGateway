/**
 * MAG Demo Dashboard — JavaScript
 * =================================
 * Calls the Admin REST API and /admin/metrics.
 * No framework; no logic of its own; no credentials stored in code.
 *
 * TODO M2: implement fetch calls for all three panels.
 */

const API_BASE = '';  // same origin as the gateway

function getAdminToken() {
    return document.getElementById('admin-token').value.trim();
}

function authHeaders() {
    return { 'Authorization': 'Bearer ' + getAdminToken() };
}

// ---- Panel 1: Metrics -----------------------------------------------

document.getElementById('btn-refresh-metrics').addEventListener('click', async () => {
    // TODO M2: fetch GET /admin/metrics and populate metric-* spans
    document.getElementById('metric-allowed').textContent = '(not yet implemented)';
});

// ---- Panel 2: Routes ------------------------------------------------

async function loadRoutes() {
    // TODO M2: fetch GET /admin/routes and render as table
    document.getElementById('routes-list').textContent = '(not yet implemented)';
}

loadRoutes();

// ---- Panel 3: Request Tester ----------------------------------------

document.getElementById('btn-send-request').addEventListener('click', async () => {
    const method = document.getElementById('tester-method').value;
    const path   = document.getElementById('tester-path').value;
    const token  = document.getElementById('tester-token').value;
    const result = document.getElementById('tester-result');

    // TODO M2: send fetch to the gateway with the chosen method/path/token,
    //          display response status and headers in tester-result.
    result.textContent = '(not yet implemented)';
});
