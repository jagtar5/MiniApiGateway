/**
 * tools/load_test.js — k6 load-test script for MAG
 * ==================================================
 * Measures p99 gateway overhead at 500 req/s (MAG-QR-08).
 *
 * Prerequisite: k6 installed (https://k6.io)
 * Usage:
 *   k6 run --vus 50 --rps 500 --duration 60s tools/load_test.js
 *
 * Environment variables (set before running):
 *   MAG_BASE_URL  — gateway base URL, e.g. http://localhost:8080
 *   MAG_JWT       — a valid signed JWT for authenticated routes
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const gatewayLatency = new Trend('gateway_added_latency_ms', true);

const BASE_URL = __ENV.MAG_BASE_URL || 'http://localhost:8080';
const JWT      = __ENV.MAG_JWT      || '';

export const options = {
    // TODO M2: calibrate against measured baseline (MAG-QR-08)
    vus: 50,
    duration: '60s',
    thresholds: {
        // p99 latency added by the gateway must be ≤ 10 ms (MAG-QR-08)
        // Actual threshold requires subtracting upstream direct latency; calibrated at M2.
        'http_req_duration{scenario:default}': ['p(99)<500'],  // placeholder until baseline
    },
};

export default function () {
    const params = { headers: { Authorization: `Bearer ${JWT}` } };

    // ---- Test authenticated fast path ----
    const res = http.get(`${BASE_URL}/fast`, params);
    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // TODO M2: add rate-limit boundary test (N, N+1 requests)
    // TODO M2: add circuit-breaker failure injection test
}
