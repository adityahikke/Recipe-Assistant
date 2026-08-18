import { chromium } from 'playwright';
import { mkdir } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.join(__dirname, '..', 'docs', 'screenshots');
const baseUrl = process.env.APP_URL || 'http://localhost:8080';

async function capture(page, name) {
  await page.screenshot({ path: path.join(outDir, name), fullPage: false });
  console.log('Saved', name);
}

async function main() {
  await mkdir(outDir, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

  await page.goto(baseUrl, { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  await capture(page, '01-home.png');

  await page.fill('#ingredients', 'chicken, garlic, lemon, olive oil');
  await page.click('#generateBtn');
  await page.waitForSelector('#resultsPanel:not(.hidden) #copyBtn', { timeout: 120000 });
  await page.locator('#resultsPanel').scrollIntoViewIfNeeded();
  await page.waitForTimeout(600);
  await capture(page, '02-recipe-result.png');

  await page.click('#libraryBtn');
  await page.waitForSelector('#libraryModal:not(.hidden)', { timeout: 10000 });
  await page.waitForTimeout(500);
  await capture(page, '03-library.png');

  await browser.close();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
