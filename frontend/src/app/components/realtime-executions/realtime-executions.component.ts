import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { EpitopeTaskData } from '../../models/EpitopeTaskData';
import { EpitopesService } from '../../services/epitopes/epitopes.service';
import { LoginService } from '../../services/login/login.service';

@Component({
  selector: 'app-realtime-executions',
  standalone: false,
  templateUrl: './realtime-executions.component.html',
  styleUrls: ['./realtime-executions.component.scss']
})
export class RealtimeExecutionsComponent implements OnInit, OnDestroy {
  processes: EpitopeTaskData[] = [];
  columns: string[] = ['PID', 'Task name', 'Started At', 'Status'];
  userId: number | undefined;
  taskListChangedSubscription: Subscription | undefined;

  constructor(
    private epitopesService: EpitopesService,
    private loginService: LoginService
  ) { }

  ngOnInit() {
    this.userId = this.loginService.getUser()?.id;
    if (this.userId !== undefined) {
      this.loadTasks();  // Carrega as tarefas inicialmente
      // Inscreve-se no observable de mudanças da lista de tarefas
      this.taskListChangedSubscription = this.epitopesService.taskListChanged$.subscribe(() => {
        this.loadTasks();  // Recarrega as tarefas quando há uma mudança
      });
    } else {
      console.error("User ID is undefined");
    }
  }

  ngOnDestroy() {
    if (this.taskListChangedSubscription) {
      this.taskListChangedSubscription.unsubscribe();
    }
  }

  loadTasks(): void {
    this.userId = this.loginService.getUser()?.id;
    if (this.userId !== undefined) {
      this.epitopesService
        .getExecutedTasksByUserIdAndStatus(this.userId)
        .subscribe((tasks) => {
          this.processes = tasks;
        });
    }
  }
}
