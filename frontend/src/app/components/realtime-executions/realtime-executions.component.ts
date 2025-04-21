import { Component } from '@angular/core';
import { EpitopeTaskData } from '../../models/EpitopeTaskData';
import { EpitopesService } from '../../services/epitopes/epitopes.service';
import { LoginService } from '../../services/login/login.service';

@Component({
  selector: 'app-realtime-executions',
  standalone: false,
  templateUrl: './realtime-executions.component.html',
  styleUrls: ['./realtime-executions.component.scss']
})
export class RealtimeExecutionsComponent {
  processes: EpitopeTaskData[] = [];
  columns: string[] = ['PID', 'Task name', 'Started At', 'Status'];

  constructor(private epitopesService: EpitopesService, private loginService: LoginService) { }

  ngOnInit() {
    const userId = this.loginService.getUser()?.id;
    if (userId !== undefined) {
      this.epitopesService.getExecutedTasksByUserIdAndStatus(userId).subscribe((tasks) => {
        this.processes = tasks;
        console.log(tasks)
      });
    } else {
      console.error("User ID is undefined");
    }

    console.log("Processes: ", this.processes);
  }
}
