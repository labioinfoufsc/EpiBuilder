import { Component, OnInit, ChangeDetectorRef } from "@angular/core";
import { Router } from "@angular/router";
import { LoginService } from "../../services/login/login.service";
import { User } from "../../models/User";

@Component({
  selector: "app-navbar",
  standalone: false,
  templateUrl: "./navbar.component.html",
  styleUrls: ["./navbar.component.scss"],
})
export class NavbarComponent implements OnInit {
  systemName: string = "EpiBuilder";
  user: User | undefined;
  currentPage: string = "";

  constructor(
    private loginService: LoginService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  /**
   * Initializes component data on load.
   */
  ngOnInit(): void {
    this.user = this.loginService.getUser() || undefined;
    this.currentPage = this.router.url.substring(1); // Sets the current page based on the URL
  }

  /**
   * Checks if the user is logged in.
   * @returns True if logged in, otherwise false.
   */
  isLoggedIn(): boolean {
    return this.loginService.getUser() !== null;
  }

  /**
   * Navigates to the selected page if it's different from the current one.
   * @param page - The page to navigate to.
   */
  goToPage(page: string): void {
    if (this.currentPage !== page) {
      this.currentPage = page;
      this.router.navigate([`/${page}`]).then(() => {
        this.cdr.detectChanges(); // Forces UI update
      });
    }
  }

  /**
   * Checks if the given page is the active one.
   * @param page - The page to check.
   * @returns True if the given page is active, otherwise false.
   */
  isActive(page: string): boolean {
    return this.currentPage === page;
  }

  /**
   * Logs out the user and redirects to the login page.
   */
  onLogout(): void {
    this.loginService.logout();
    this.router.navigate(["/login"]).then(() => {
      this.cdr.detectChanges(); // Ensures UI updates
    });
  }
}
