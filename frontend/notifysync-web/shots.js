const puppeteer = require('puppeteer');

const OUT = '/tmp/ns-shots';
const BASE = 'http://127.0.0.1:4200';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  const fs = require('fs');
  fs.mkdirSync(OUT, { recursive: true });

  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1366, height: 900 });

  const shot = async (name) => {
    await page.screenshot({ path: `${OUT}/${name}.png` });
    console.log('shot:', name);
  };

  // 1. Login page
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle0' });
  await sleep(800);
  await shot('01-login');

  // 2. Fill + submit login (demo user seeded via API earlier)
  await page.type('input[formcontrolname="email"]', 'demo@notifysync.io');
  await page.type('input[formcontrolname="password"]', 'Passw0rd!');
  await Promise.all([
    page.click('button.submit'),
    page.waitForNavigation({ waitUntil: 'networkidle0' }).catch(() => {}),
  ]);
  await sleep(1500);
  await shot('02-dashboard');

  // 3. Notifications
  await page.goto(`${BASE}/notifications`, { waitUntil: 'networkidle0' });
  await sleep(1200);
  await shot('03-notifications');

  // 4. Devices
  await page.goto(`${BASE}/devices`, { waitUntil: 'networkidle0' });
  await sleep(1200);
  await shot('04-devices');

  // 5. Settings
  await page.goto(`${BASE}/settings`, { waitUntil: 'networkidle0' });
  await sleep(1000);
  await shot('05-settings');

  // 6. Dark mode dashboard
  await page.goto(`${BASE}/dashboard`, { waitUntil: 'networkidle0' });
  await sleep(800);
  // click the theme toggle (dark_mode icon button in toolbar)
  await page.evaluate(() => {
    const btns = [...document.querySelectorAll('mat-toolbar button')];
    const t = btns.find((b) => b.textContent.includes('dark_mode'));
    if (t) t.click();
  });
  await sleep(1000);
  await shot('06-dashboard-dark');

  await browser.close();
  console.log('DONE');
})().catch((e) => { console.error('SHOT ERROR:', e); process.exit(1); });
