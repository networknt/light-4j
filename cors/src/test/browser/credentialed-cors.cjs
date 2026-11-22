// Run through CorsBrowserSmokeTest so this exercises the actual Java CORS handler.
const assert = require('node:assert/strict');
const { chromium } = require(process.env.CORS_PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const port = Number(process.argv[2]);
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.CORS_CHROMIUM_EXECUTABLE || undefined,
    args: ['--no-proxy-server', '--host-resolver-rules=MAP *.example.com 127.0.0.1, MAP example.com 127.0.0.1, MAP badexample.com 127.0.0.1'],
  });
  try {
    const context = await browser.newContext();
    await context.addCookies([{ name: 'cors-smoke', value: 'present', domain: 'api.example.com',
      path: '/', httpOnly: true, secure: false, sameSite: 'Lax' }]);
    const page = await context.newPage();
    const target = `http://api.example.com:${port}/data`;
    for (const host of ['app.example.com', 'team.app.example.com']) {
      await page.goto(`http://${host}:${port}/browser`);
      for (const method of ['GET', 'PUT']) {
        const result = await page.evaluate(async ({ target, method }) => {
          const response = await fetch(target, { method, credentials: 'include',
            headers: method === 'PUT' ? { 'X-Cors-Smoke': 'preflight' } : {} });
          return { status: response.status, text: await response.text() };
        }, { target, method });
        assert.deepEqual(result, { status: 200, text: 'credential-ok' });
      }
    }
    const omitted = await page.evaluate(async target => (await fetch(target, { credentials: 'omit' })).status, target);
    assert.equal(omitted, 401, 'fixture must require a credential');
    for (const host of ['example.com', 'badexample.com']) {
      await page.goto(`http://${host}:${port}/browser`);
      const blocked = await page.evaluate(async target => {
        try { await fetch(target, { method: 'PUT', credentials: 'include', headers: { 'X-Cors-Smoke': 'preflight' } }); return false; }
        catch { return true; }
      }, target);
      assert.equal(blocked, true, `${host} must not receive credentialed CORS access`);
    }
    console.log('PASS: credentialed GET and preflighted PUT, nested subdomains, apex and boundary rejection');
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
