import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BackendService } from './services/backend/backend.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  title = 'EpiBuilder 2.0.0';
  showNavbar = false;
  isBackendReady = false;

  constructor(private router: Router, private backendService: BackendService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.showNavbar = !event.url.includes('/login');
      }
    });
  }

  ngOnInit(): void {
    const interval = setInterval(() => {
      this.backendService.checkStatus().subscribe({
        next: (res) => {
          this.isBackendReady = res.status === 'ready';
        },
        error: (res) => {
          this.isBackendReady = false;
        }
      });
    }, 3000); 
  }
}
