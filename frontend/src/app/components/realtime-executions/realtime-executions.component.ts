import { Component } from '@angular/core';
import { EpitopesService } from '../../services/epitopes/epitopes.service';
import { EpitopeTaskData } from '../../models/EpitopeTaskData';
import { LoginService } from '../../services/login/login.service';

@Component({
  selector: 'app-realtime-executions',
  standalone: false,
  templateUrl: './realtime-executions.component.html',
  styleUrls: ['./realtime-executions.component.css']
})
export class RealtimeExecutionsComponent {
  processes: EpitopeTaskData[] = [];
  columns: string[] = ['PID', 'Task name', 'Processing time', 'Progress'];

  constructor(private epitopesService: EpitopesService, private loginService: LoginService) {}

  ngOnInit() {
    const userId = this.loginService.getUser()?.id;
    if (userId !== undefined) {
      this.epitopesService.getExecutedTasksByUserId(userId).subscribe((tasks) => {
        this.processes = tasks;
      });
    } else {
      console.error("User ID is undefined");
    }

    console.log("Processes: ", this.processes);
  }
}
