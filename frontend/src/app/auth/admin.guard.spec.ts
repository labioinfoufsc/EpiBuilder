import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminGuard } from './admin.guard';
import { LoginService } from '../services/login/login.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { User } from '../models/User';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let loginService: LoginService;
  let router: Router;

  

  const mockRouter = {
    navigate: jasmine.createSpy('navigate')
  };

  const mockLoginService = {
    getUser: jasmine.createSpy('getUser')
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClientTesting(),
        AdminGuard,
        { provide: Router, useValue: mockRouter },
        { provide: LoginService, useValue: mockLoginService }
      ]
    });

    guard = TestBed.inject(AdminGuard);
    loginService = TestBed.inject(LoginService);
    router = TestBed.inject(Router);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access if user has the required role', () => {
    const user: User = {
      id: 1,
      username: 'adminUser',
      name: 'Admin User',
      role: 'admin'
    };
    mockLoginService.getUser.and.returnValue(user);

    const route = { data: { role: 'admin' } } as unknown as ActivatedRouteSnapshot;
    const state = { url: '/dashboard' } as RouterStateSnapshot;

    expect(guard.canActivate(route, state)).toBeTrue();
  });

  it('should deny access and navigate to root if user does not have the required role', () => {
    const user: User = {
      id: 2,
      username: 'regularUser',
      name: 'Regular User',
      role: 'user'
    };
    mockLoginService.getUser.and.returnValue(user);

    const route = { data: { role: 'admin' } } as unknown as ActivatedRouteSnapshot;
    const state = { url: '/admin' } as RouterStateSnapshot;

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should redirect authenticated users trying to access the login page', () => {
    const user: User = {
      id: 3,
      username: 'authUser',
      name: 'Authenticated User',
      role: 'user'
    };
    mockLoginService.getUser.and.returnValue(user);

    const route = { data: {} } as unknown as ActivatedRouteSnapshot;
    const state = { url: '/login' } as RouterStateSnapshot;

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/new']);
  });

  it('should allow access if there is no required role', () => {
    const user: User = {
      id: 4,
      username: 'flexibleUser',
      name: 'Flexible User',
      role: 'user'
    };
    mockLoginService.getUser.and.returnValue(user);

    const route = { data: {} } as unknown as ActivatedRouteSnapshot;
    const state = { url: '/dashboard' } as RouterStateSnapshot;

    expect(guard.canActivate(route, state)).toBeTrue();
  });
});
