import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { LoginService } from '../services/login/login.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  constructor(private loginService: LoginService, private router: Router) {}

  /**
   * Determines if the user is allowed to activate the route.
   * 
   * Verifies if the user is logged in, and if their role matches the required role for the route.
   * If the user is not logged in or their role does not match, navigation is blocked.
   * If the user is logged in and trying to access the login page, they are redirected to the home page.
   * 
   * @param route The activated route snapshot, containing route parameters and data.
   * @param state The router state snapshot, representing the current state of the router.
   * @returns boolean indicating whether the route can be activated or not.
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const user = this.loginService.getUser();
    const isLoggedIn = user !== null;
    const userRole = user?.role;
    const requiredRole = route.data['role'];

    // If the user is logged in and trying to access the login page, redirect to 'new' page
    if (isLoggedIn && state.url === '/login') {
      this.router.navigate(['/new']);
      return false;
    }

    // If the user is not logged in, redirect to login page
    if (!isLoggedIn) {
      this.router.navigate(['/login']);
      return false;
    }

    // If the route requires a specific role and the user's role doesn't match, redirect to the home page
    if (requiredRole && userRole !== requiredRole) {
      this.router.navigate(['/']);
      return false;
    }

    return true;
  }
}
