import { spawnSync, spawn } from 'node:child_process';
import { migrateSqlite } from './migrate-sqlite.mjs';
migrateSqlite();
const windows = process.platform === 'win32';
const mvn = windows ? 'backend\\mvnw.cmd' : './backend/mvnw';
const build = spawnSync(mvn, ['-q', '-f', 'backend/pom.xml', '-DskipTests', 'package'], {
  stdio: 'inherit',
  shell: windows,
});
if (build.error) {
  console.error(build.error.message);
  process.exit(1);
}
if (build.status !== 0) process.exit(build.status || 1);
const child = spawn('java', ['-jar', 'backend/target/corte-agil.jar'], {
  stdio: 'inherit',
  env: process.env,
});
child.on('error', (error) => {
  console.error(error.message);
  process.exitCode = 1;
});
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => child.kill(signal));
child.on('exit', (code) => {
  process.exitCode = code || 0;
});
