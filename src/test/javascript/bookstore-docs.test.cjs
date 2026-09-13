const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function page(fetch, contextPath = '') {
    let config;
    const bundle = (value) => { config = value; return {}; };
    bundle.presets = { apis: {} };
    const context = vm.createContext({
        window: { location: { href: `http://localhost:8080${contextPath}/docs/index.html` } },
        URL, fetch, SwaggerUIBundle: bundle, SwaggerUIStandalonePreset: {}
    });
    vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/docs/bookstore-docs.js'), 'utf8'), context);
    return config;
}

test('reads use the browser session without fetching CSRF', async () => {
    const config = page(() => { assert.fail('GET must not fetch a token'); });
    const result = await config.requestInterceptor({ url: 'http://localhost:8080/api/v1/cart', method: 'GET' });
    assert.equal(result.credentials, 'same-origin');
    assert.equal(config.url, 'http://localhost:8080/v3/api-docs');
});

test('every mutation obtains a fresh token, including after login/logout', async () => {
    let calls = 0;
    const config = page(async (url, options) => {
        assert.equal(url.href, 'http://localhost:8080/api/v1/authentication/csrf');
        assert.equal(options.credentials, 'same-origin');
        assert.equal(options.cache, 'no-store');
        return { ok: true, json: async () => ({ headerName: 'X-CSRF-TOKEN', token: `token-${++calls}` }) };
    });
    for (const [index, method] of ['POST', 'PUT', 'PATCH', 'DELETE'].entries()) {
        const result = await config.requestInterceptor({ url: 'http://localhost:8080/api/v1/cart/items', method,
            headers: { 'Idempotency-Key': 'preserved' } });
        assert.equal(result.headers['X-CSRF-TOKEN'], `token-${index + 1}`);
        assert.equal(result.headers['Idempotency-Key'], 'preserved');
    }
    assert.equal(calls, 4);
});

test('context paths are preserved for the specification and CSRF request', async () => {
    const config = page(async (url) => {
        assert.equal(url.href, 'http://localhost:8080/bookstore/api/v1/authentication/csrf');
        return { ok: true, json: async () => ({ headerName: 'X-CSRF-TOKEN', token: 'fresh' }) };
    }, '/bookstore');
    assert.equal(config.url, 'http://localhost:8080/bookstore/v3/api-docs');
    await config.requestInterceptor({ url: 'http://localhost:8080/bookstore/api/v1/orders', method: 'POST' });
});

test('tokens cannot be forwarded to a different origin', async () => {
    const config = page(() => { assert.fail('Foreign origin must not fetch a token'); });
    await assert.rejects(config.requestInterceptor({ url: 'https://example.com/api/v1/orders', method: 'POST' }), /same origin/);
});

test('CSRF failures stop the mutation instead of sending an unprotected request', async () => {
    for (const response of [
        { ok: false },
        { ok: true, json: async () => ({ headerName: 'X-CSRF-TOKEN', token: '' }) },
        { ok: true, json: async () => ({ headerName: 'Unexpected-Header', token: 'value' }) }
    ]) {
        const config = page(async () => response);
        await assert.rejects(config.requestInterceptor({ url: 'http://localhost:8080/api/v1/orders', method: 'POST' }), /CSRF/);
    }
});
