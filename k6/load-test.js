import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const redirectLatency = new Trend('redirect_latency', true);
const shortenLatency = new Trend('shorten_latency', true);
const cacheHitRate = new Rate('cache_hit_rate');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'sk_test_snaplink_dev_key_123';

export const options = {
    stages: [
        { duration: '10s', target: 50 },    // Ramp up to 50 VUs
        { duration: '20s', target: 200 },    // Ramp up to 200 VUs
        { duration: '30s', target: 500 },    // Sustain 500 VUs
        { duration: '10s', target: 0 },      // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<50'],     // 95% of requests < 50ms
        redirect_latency: ['p(95)<15'],      // 95% of redirects < 15ms
        http_req_failed: ['rate<0.05'],      // Error rate < 5%
    },
};

const createHeaders = { 'Content-Type': 'application/json', 'X-API-Key': API_KEY };

// Setup: create URLs using API key (higher rate limit)
export function setup() {
    const shortCodes = [];
    for (let i = 0; i < 50; i++) {
        const payload = JSON.stringify({
            url: `https://example.com/page/${i}?param=value&tracking=${Date.now()}`,
        });
        const res = http.post(`${BASE_URL}/api/v1/shorten`, payload, { headers: createHeaders });
        if (res.status === 201) {
            const body = JSON.parse(res.body);
            shortCodes.push(body.shortCode);
        }
    }
    console.log(`Setup created ${shortCodes.length} short URLs`);
    return { shortCodes };
}

export default function (data) {
    const shortCodes = data.shortCodes;

    if (shortCodes.length === 0) {
        console.error('No short codes available for testing');
        return;
    }

    // 100% redirects for load test (redirects are not rate-limited)
    const code = shortCodes[Math.floor(Math.random() * shortCodes.length)];
    const res = http.get(`${BASE_URL}/${code}`, { redirects: 0 });
    redirectLatency.add(res.timings.duration);
    cacheHitRate.add(res.status === 302);

    check(res, {
        'redirect returns 302': (r) => r.status === 302,
        'has Location header': (r) => r.headers['Location'] !== undefined,
    });

    sleep(0.01); // 10ms think time
}
