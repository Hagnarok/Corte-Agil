import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

const source = readFileSync(new URL('../public/notificacoes-sw.js', import.meta.url), 'utf8');

async function clicar(url, existingUrl) {
  const handlers = {};
  const result = { closed: false, focused: false, opened: null };
  const existing = {
    url: existingUrl,
    focus: async () => {
      result.focused = true;
    },
  };
  const self = {
    location: { origin: 'https://corte.example' },
    addEventListener: (name, handler) => {
      handlers[name] = handler;
    },
    clients: {
      matchAll: async () => (existingUrl ? [existing] : []),
      openWindow: async (target) => {
        result.opened = target;
      },
    },
  };
  runInNewContext(source, { self, URL });
  let finished;
  handlers.notificationclick({
    notification: {
      data: { url },
      close: () => {
        result.closed = true;
      },
    },
    waitUntil: (promise) => {
      finished = promise;
    },
  });
  await finished;
  return result;
}

test('clicar no aviso retorna à aba da própria senha', async () => {
  const url = 'https://corte.example/cliente#senha=abc';
  const result = await clicar(url, url);
  assert.deepEqual(result, { closed: true, focused: true, opened: null });
});

test('sem aba aberta, o aviso abre o acompanhamento da senha', async () => {
  const url = 'https://corte.example/cliente#senha=abc';
  assert.equal((await clicar(url)).opened, url);
});

test('aviso não abre outro site nem uma rota administrativa', async () => {
  for (const url of [
    'https://outro.example/cliente#senha=abc',
    'https://corte.example/funcionario',
    undefined,
  ]) {
    assert.equal((await clicar(url)).opened, 'https://corte.example/cliente');
  }
});
