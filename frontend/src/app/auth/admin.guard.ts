import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { LoginService } from '../services/login/login.service';

/**
 * AdminGuard is a route guard responsible for controlling access to specific routes
 * based on the user's authentication state and role.
 * It implements the CanActivate interface from Angular Router.
 */
@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  /**
   * Creates an instance of AdminGuard.
   * @param loginService The service responsible for user authentication and session management.
   * @param router The Angular Router used for navigation.
   */
  constructor(private loginService: LoginService, private router: Router) {}

  /**
   * Determines if the route can be activated based on the user's authentication and role.
   *
   * @param route The current activated route snapshot containing route metadata.
   * @param state The router state snapshot, containing the URL of the target route.
   * @returns A boolean value indicating whether the user can activate the requested route.
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const userRole = this.loginService.getUser()?.role;
    const requiredRole = route.data['role'];

    // Redirect authenticated users trying to access the login page.
    if (this.loginService.getUser() != null && state.url === '/login') {
      this.router.navigate(['/new']);
      return false;
    }

    // Deny access if the user's role does not match the required role for the route.
    if (requiredRole && userRole !== requiredRole) {
      this.router.navigate(['/']);
      return false;
    }

    return true;
  }
}