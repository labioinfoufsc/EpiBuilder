import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { BackendService } from './services/backend/backend.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  /** Application title displayed in the browser. */
  title = 'EpiBuilder 2.0.0';
  /** Controls the visibility of the main navigation bar. */
  showNavbar = false;
  /** Global flag indicating if the backend has finished its startup sequence. */
  isBackendReady = false;
  /** Detailed message shown on the loading screen. */
  initializationMessage: string = 'Awaiting server startup...';

  private pollingSubscription!: Subscription;

  constructor(private router: Router, private backendService: BackendService) {
    // Hides the navbar on the login page.
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.showNavbar = !event.url.includes('/login');
      }
    });
  }

  ngOnInit(): void {
    this.pollingSubscription = interval(3000).subscribe(() => {
      this.backendService.checkStatus().subscribe({
        next: (res: { status: string, message?: string }) => {
          const status = res.status.toUpperCase();

          if (status === 'READY') {
            this.isBackendReady = true;
            this.initializationMessage = 'System is ready!';
            this.pollingSubscription.unsubscribe();

          } else if (status === 'INITIALIZING_DB') {
            this.isBackendReady = false;
            this.initializationMessage = res.message || 'Downloading and preparing UniProt database. This may take several minutes (5-15 min) on first launch.';

          } else {
            this.isBackendReady = false;
            this.initializationMessage = res.message || 'Server online, but not ready. Awaiting initialization completion...';
          }
        },
        error: (err) => {
          this.isBackendReady = false;
          this.initializationMessage = 'Connection error with the backend. Attempting to reconnect...';
        }
      });
    });
  }
}