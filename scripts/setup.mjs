import { existsSync, writeFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
if (existsSync('.env')) console.log('O arquivo .env já existe. A senha foi preservada.');
else {
  const password = randomBytes(12).toString('base64url');
  writeFileSync('.env', `PORT=3000\nHOST=0.0.0.0\nADMIN_PASSWORD=${password}\n`, { mode: 0o600 });
  console.log(
    `Configuração criada.\nUsuário: admin\nSenha: ${password}\nGuarde a senha. Ela também está no arquivo .env.`,
  );
}
