import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: false,
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  systemName: string = 'EpiBuilder';
  currentYear: number = new Date().getFullYear();
  currentVersion: string = '2.0.0';

}
