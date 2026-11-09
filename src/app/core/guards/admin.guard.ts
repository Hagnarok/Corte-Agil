import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
export const adminGuard: CanActivateFn = async () => {
  const auth = inject(AuthService),
    router = inject(Router);
  return (await auth.check()) && auth.perfil() === 'ADMIN' ? true : router.parseUrl('/funcionario');
};
