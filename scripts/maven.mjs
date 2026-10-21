import { spawnSync } from 'node:child_process';
const windows = process.platform === 'win32';
const result = spawnSync(
  windows ? 'backend\\mvnw.cmd' : './backend/mvnw',
  ['-f', 'backend/pom.xml', ...process.argv.slice(2)],
  { stdio: 'inherit', shell: windows },
);
if (result.error) console.error(result.error.message);
process.exit(result.status ?? 1);
