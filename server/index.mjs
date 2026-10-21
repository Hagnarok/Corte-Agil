import express from 'express';
import { DatabaseSync } from 'node:sqlite';
import { mkdirSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';
import * as queue from './domain.mjs';

const password = process.env.ADMIN_PASSWORD;
if (!password || password.length < 10)
  throw Error('Execute npm run setup ou configure ADMIN_PASSWORD com 10 ou mais caracteres.');
const dir = resolve(process.env.DATA_DIR || 'data');
mkdirSync(dir, { recursive: true });
const db = new DatabaseSync(resolve(dir, 'corte-agil.sqlite'));
db.exec(
  'PRAGMA journal_mode=WAL; PRAGMA synchronous=FULL; CREATE TABLE IF NOT EXISTS queue_state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL);',
);
db.prepare('INSERT OR IGNORE INTO queue_state VALUES(1, ?)').run(
  JSON.stringify(queue.initialState()),
);
const read = () =>
  JSON.parse(db.prepare('SELECT payload FROM queue_state WHERE id=1').get().payload);
const streams = new Set(),
  sessions = new Map(),
  rates = new Map();
const salt = randomBytes(16),
  hash = scryptSync(password, salt, 64);
const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '8kb' }));
app.use((req, res, next) => {
  res.set({
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'no-referrer',
    'X-Frame-Options': 'DENY',
  });
  if (req.path.startsWith('/api')) res.set('Cache-Control', 'no-store');
  next();
});
// Origens públicas explícitas: o proxy HTTPS do Codespaces chega ao Node via HTTP.
const allowedOrigins = new Set(
  (process.env.ALLOWED_ORIGINS || '')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean),
);
if (process.env.CODESPACE_NAME && process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN) {
  for (const port of [Number(process.env.PORT) || 3000, 4200]) {
    allowedOrigins.add(
      `https://${process.env.CODESPACE_NAME}-${port}.${process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN}`,
    );
  }
}
if (process.env.NODE_ENV !== 'production') {
  allowedOrigins.add('http://localhost:4200');
  allowedOrigins.add('http://127.0.0.1:4200');
}
app.use('/api', (req, res, next) => {
  if (
    req.method === 'POST' &&
    req.headers.origin &&
    req.headers.origin !== `${req.protocol}://${req.headers.host}` &&
    !allowedOrigins.has(req.headers.origin)
  ) {
    // Não registrar cookies, credenciais ou o corpo da requisição.
    console.warn(
      '[origem bloqueada]',
      JSON.stringify({
        origin: req.headers.origin,
        host: req.headers.host,
        protocol: req.protocol,
        allowedOrigins: [...allowedOrigins],
      }),
    );
    return res.status(403).json({ message: 'Origem não permitida.' });
  }
  next();
});
function limit(prefix, max) {
  return (req, res, next) => {
    const key = prefix + req.ip,
      now = Date.now();
    let e = rates.get(key);
    if (!e || e.until < now) {
      e = { count: 0, until: now + 60000 };
      rates.set(key, e);
    }
    if (++e.count > max)
      return res.status(429).json({ message: 'Muitas solicitações. Aguarde um minuto.' });
    next();
  };
}
function auth(req, res, next) {
  const sid = req.headers.cookie
    ?.split(';')
    .map((s) => s.trim())
    .find((s) => s.startsWith('ca_session='))
    ?.slice(11);
  const session = sessions.get(sid);
  if (!session || session < Date.now())
    return res.status(401).json({ message: 'Entre como funcionário para continuar.' });
  req.sessionId = sid;
  next();
}
function sendSnapshot(res) {
  res.write(`data: ${JSON.stringify(queue.snapshot(read()))}\n\n`);
}
function broadcast() {
  for (const res of streams) {
    try {
      sendSnapshot(res);
    } catch {
      streams.delete(res);
      res.end();
    }
  }
}
function mutate(fn) {
  db.exec('BEGIN IMMEDIATE');
  try {
    const state = read();
    const result = fn(state);
    db.prepare('UPDATE queue_state SET payload=? WHERE id=1').run(JSON.stringify(state));
    db.exec('COMMIT');
    broadcast();
    return result;
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}
app.post('/api/login', limit('login', 8), (req, res) => {
  const candidate = typeof req.body.password === 'string' ? req.body.password : '';
  if (req.body.username !== 'admin' || !timingSafeEqual(hash, scryptSync(candidate, salt, 64)))
    return res.status(401).json({ message: 'Usuário ou senha incorretos.' });
  const sid = randomBytes(32).toString('hex');
  sessions.set(sid, Date.now() + 8 * 3600000);
  res.cookie('ca_session', sid, {
    httpOnly: true,
    sameSite: 'strict',
    secure: req.secure,
    maxAge: 8 * 3600000,
  });
  res.json({ ok: true });
});
app.get('/api/session', auth, (req, res) => res.json({ username: 'admin' }));
app.post('/api/logout', auth, (req, res) => {
  sessions.delete(req.sessionId);
  res.clearCookie('ca_session');
  res.json({ ok: true });
});
app.get('/api/state', (req, res) => res.json(queue.snapshot(read())));
app.get('/api/events', (req, res) => {
  res.set({
    'Content-Type': 'text/event-stream',
    Connection: 'keep-alive',
    'Cache-Control': 'no-cache',
  });
  res.flushHeaders();
  streams.add(res);
  sendSnapshot(res);
  req.on('close', () => streams.delete(res));
});
app.post('/api/tickets', limit('issue', 60), (req, res) => {
  const t = mutate((s) =>
    queue.issue(s, req.body.service ?? 'Não informado', req.body.origin ?? 'app'),
  );
  res.status(201).json({ token: t.token, label: t.label });
});
app.get('/api/tickets/:token', (req, res) => res.json(queue.ownTicket(read(), req.params.token)));
app.post('/api/tickets/:token/return', limit('return', 20), (req, res) =>
  res.json(mutate((s) => queue.rejoin(s, req.params.token))),
);
app.post('/api/queue/call', auth, (req, res) =>
  res.json(queue.publicTicket(mutate((s) => queue.callNext(s)))),
);
app.post('/api/queue/:action', auth, (req, res) => {
  if (req.params.action === 'pause') {
    mutate((s) => {
      s.paused = !s.paused;
      queue.audit(s, s.paused ? 'Emissão pausada' : 'Emissão retomada', null, Date.now());
    });
    return res.json({ ok: true });
  }
  res.json(queue.publicTicket(mutate((s) => queue.act(s, req.params.action))));
});
app.get('/api/audit', auth, (req, res) => res.json(read().audit.slice(-100).reverse()));
const dist = resolve('dist/corte-agil/browser');
if (existsSync(dist)) {
  app.use(express.static(dist));
  app.get('/{*path}', (req, res, next) =>
    req.path.startsWith('/api/') ? next() : res.sendFile(resolve(dist, 'index.html')),
  );
}
app.use('/api', (req, res) => res.status(404).json({ message: 'Rota não encontrada.' }));
app.use((error, req, res, next) => {
  if (!error.status || error.status >= 500) console.error(error);
  res.status(error.status || 500).json({
    message: error.status ? error.message : 'Falha interna. Tente novamente.',
  });
});
const heartbeat = setInterval(() => {
  for (const res of streams) res.write(': heartbeat\n\n');
  const now = Date.now();
  for (const [key, value] of rates) if (value.until < now) rates.delete(key);
  for (const [key, value] of sessions) if (value < now) sessions.delete(key);
}, 15000);
const server = app.listen(Number(process.env.PORT) || 3000, process.env.HOST || '0.0.0.0', () =>
  console.log(
    `CorteÁgil disponível na porta ${server.address().port}. Acesse http://localhost:${server.address().port}`,
  ),
);
function close() {
  clearInterval(heartbeat);
  for (const res of streams) res.end();
  server.close(() => {
    db.close();
    process.exit(0);
  });
}
process.on('SIGTERM', close);
process.on('SIGINT', close);
