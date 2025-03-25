import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { LoginService } from '../services/login/login.service';
@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(private loginService: LoginService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const isLoggedIn = this.loginService.getUser() !== null;
    const userRole = this.loginService.getUser()?.role;
    const requiredRole = route.data['role']; // Pega a role esperada da configuração da rota

    // Se o usuário já está logado e tenta acessar login, redireciona para home
    if (isLoggedIn && state.url === '/login') {
      this.router.navigate(['/new']);
      return false;
    }

    // Se a rota exige uma role específica e o usuário não tem essa role, bloqueia
    if (requiredRole && userRole !== requiredRole) {
      this.router.navigate(['/']); // Redireciona para home ou outra página segura
      return false;
    }

    return true;
  }
}