// Chrome 헤드리스를 DevTools 프로토콜로 몰아 PDF 를 만든다.
//
// --print-to-pdf 로는 푸터 서식을 지정할 수 없다. 기본 푸터를 켜면 파일 경로와
// 날짜가 찍혀 지저분하고, 끄면 쪽 번호까지 사라진다. CSS @page 의 counter(page) 는
// Chrome 헤드리스가 지원하지 않으므로 프로토콜의 footerTemplate 을 쓴다.
//
// 사용: node tools/print_pdf.js <html 경로> <pdf 경로> "<푸터 왼쪽 문구>"
const { spawn } = require('child_process');
const http = require('http');
const fs = require('fs');

const [htmlPath, pdfPath, label] = process.argv.slice(2);
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const PORT = 9223;

const chrome = spawn(CHROME, [
  '--headless', '--disable-gpu', `--remote-debugging-port=${PORT}`,
  '--no-first-run', '--no-default-browser-check', 'about:blank',
], { stdio: 'ignore' });

const get = (path) => new Promise((res, rej) => {
  http.get({ host: '127.0.0.1', port: PORT, path }, (r) => {
    let b = ''; r.on('data', (c) => (b += c)); r.on('end', () => res(JSON.parse(b)));
  }).on('error', rej);
});

const waitFor = async (fn, tries = 60) => {
  for (let i = 0; i < tries; i++) {
    try { return await fn(); } catch { await new Promise((r) => setTimeout(r, 250)); }
  }
  throw new Error('Chrome 응답 없음');
};

(async () => {
  // 브라우저 타깃에는 Page 도메인이 없다. 페이지 타깃을 골라야 한다.
  const targets = await waitFor(async () => {
    const list = await get('/json/list');
    const page = list.find((t) => t.type === 'page' && t.webSocketDebuggerUrl);
    if (!page) throw new Error('페이지 타깃 없음');
    return [page];
  });
  const WebSocket = require('ws');
  const ws = new WebSocket(targets[0].webSocketDebuggerUrl, { maxPayload: 512 * 1024 * 1024 });
  let id = 0;
  const pending = new Map();
  const events = new Map();
  ws.on('message', (raw) => {
    const m = JSON.parse(raw);
    if (m.id && pending.has(m.id)) {
      const { res, rej } = pending.get(m.id); pending.delete(m.id);
      m.error ? rej(new Error(m.error.message)) : res(m.result);
    } else if (m.method && events.has(m.method)) {
      const fn = events.get(m.method); events.delete(m.method); fn();
    }
  });
  const send = (method, params) => new Promise((res, rej) => {
    const mid = ++id;
    pending.set(mid, { res, rej });
    ws.send(JSON.stringify({ id: mid, method, params }));
  });
  const once = (evt) => new Promise((res) => events.set(evt, res));

  await new Promise((r) => ws.on('open', r));
  await send('Page.enable');
  const loaded = once('Page.loadEventFired');
  await send('Page.navigate', { url: 'file://' + htmlPath });
  await loaded;
  await new Promise((r) => setTimeout(r, 1500));   // 폰트·이미지 대기

  // 푸터 프레임은 @font-face 를 무시한다. 지정할 수 있는 것은 시스템에 이미
  // 있는 폰트뿐이라, 이름을 안 적으면 한글이 명조로 떨어진다.
  const style = "font-size:8px; font-family:'Apple SD Gothic Neo','Helvetica Neue',Arial,sans-serif;"
    + " color:#8a94a6; width:100%; padding:0 13mm;";
  const { data } = await send('Page.printToPDF', {
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: '<div></div>',
    footerTemplate:
      `<div style="${style}; display:flex; justify-content:space-between;">` +
      `<span>${label}</span>` +
      '<span><span class="pageNumber"></span> / <span class="totalPages"></span></span>' +
      '</div>',
    // CDP 는 여백을 인치 숫자로 받는다. 16mm=0.63, 18mm=0.71, 13mm=0.51
    marginTop: 0.63, marginBottom: 0.71, marginLeft: 0.51, marginRight: 0.51,
    paperWidth: 8.27, paperHeight: 11.69,
  });
  fs.writeFileSync(pdfPath, Buffer.from(data, 'base64'));
  ws.close(); chrome.kill();
  console.log('OK ' + (fs.statSync(pdfPath).size / 1024).toFixed(1) + 'KB');
  process.exit(0);
})().catch((e) => { console.error(e.message); chrome.kill(); process.exit(1); });
