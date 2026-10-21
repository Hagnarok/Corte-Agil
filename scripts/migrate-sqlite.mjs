import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { DatabaseSync } from 'node:sqlite';

export function migrateSqlite() {
  mkdirSync('data', { recursive: true });
  if (!existsSync('data/corte-agil.sqlite') || existsSync('data/legacy-queue.json')) return;
  const db = new DatabaseSync('data/corte-agil.sqlite', { readOnly: true });
  try {
    const row = db.prepare('SELECT payload FROM queue_state WHERE id=1').get();
    if (row) writeFileSync('data/legacy-queue.json', row.payload, { mode: 0o600, flag: 'wx' });
  } finally {
    db.close();
  }
}
