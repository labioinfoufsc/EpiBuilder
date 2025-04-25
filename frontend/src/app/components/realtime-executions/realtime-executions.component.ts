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
  columns: string[] = ['PID', 'Task name', 'Started At', 'Elapsed Time', 'Status', 'View log'];
  userId: number | undefined;
  taskListChangedSubscription: Subscription | undefined;
  logText: string = '';
  modalVisible: boolean = false;
  currentProcess: EpitopeTaskData | undefined;

  private tableUpdateInterval = 60000;
  private logUpdateInterval = 10000;
  private tableUpdateTimer: any;
  private logUpdateTimer: any;
  private elapsedTimeUpdateTimer: any;

  constructor(
    private epitopesService: EpitopesService,
    private loginService: LoginService
  ) { }

  ngOnInit() {
    this.userId = this.loginService.getUser()?.id;
    if (this.userId !== undefined) {
      this.loadTasks();

      this.startTableUpdates();

      this.taskListChangedSubscription = this.epitopesService.taskListChanged$.subscribe(() => {
        this.loadTasks();
      });

      this.startElapsedTimeUpdates();
    } else {
      console.error("User ID is undefined");
    }
  }

  ngOnDestroy() {
    this.cleanUpIntervals();
    if (this.taskListChangedSubscription) {
      this.taskListChangedSubscription.unsubscribe();
    }
  }

  private startTableUpdates(): void {
    this.cleanUpTableInterval();

    this.tableUpdateTimer = setInterval(() => {
      this.loadTasks();
    }, this.tableUpdateInterval);
  }

  private startElapsedTimeUpdates(): void {
    this.elapsedTimeUpdateTimer = setInterval(() => {
      this.updateElapsedTime();
    }, 1000);
  }

  private updateElapsedTime(): void {
    const now = new Date();

    this.processes.forEach(process => {
      process.elapsedTime = this.calculateElapsedTime(process.executionDate, now);
    });
  }

  private startLogUpdates(): void {
    this.cleanUpLogInterval();

    this.logUpdateTimer = setInterval(() => {
      this.updateLogContent();
    }, this.logUpdateInterval);
  }

  private cleanUpTableInterval(): void {
    if (this.tableUpdateTimer) {
      clearInterval(this.tableUpdateTimer);
      this.tableUpdateTimer = undefined;
    }
  }

  private cleanUpLogInterval(): void {
    if (this.logUpdateTimer) {
      clearInterval(this.logUpdateTimer);
      this.logUpdateTimer = undefined;
    }
  }

  private cleanUpElapsedTimeInterval(): void {
    if (this.elapsedTimeUpdateTimer) {
      clearInterval(this.elapsedTimeUpdateTimer);
      this.elapsedTimeUpdateTimer = undefined;
    }
  }

  private cleanUpIntervals(): void {
    this.cleanUpTableInterval();
    this.cleanUpLogInterval();
    this.cleanUpElapsedTimeInterval();
  }

  viewLog(process: EpitopeTaskData): void {
    this.currentProcess = process;
    this.modalVisible = true;
    this.updateLogContent();

    this.startLogUpdates();
  }

  closeModal(): void {
    this.modalVisible = false;
    this.cleanUpLogInterval();
  }

  private updateLogContent(): void {
    if (!this.currentProcess?.id) return;

    this.epitopesService.getTaskLog(this.currentProcess.id).subscribe({
      next: (logBlob: Blob) => {
        const reader = new FileReader();
        reader.onload = () => {
          this.logText = reader.result as string;
        };
        reader.onerror = (error) => {
          this.logText = 'Error loading log: ' + error;
        };
        reader.readAsText(logBlob);
      },
      error: (err: { error: any; message: any }) => {
        this.logText = 'Error loading log: ' + (err.error || err.message);
      }
    });
  }

  loadTasks(): void {
    if (this.userId === undefined) {
      this.userId = this.loginService.getUser()?.id;
      if (this.userId === undefined) return;
    }

    this.epitopesService
      .getExecutedTasksByUserIdAndStatus(this.userId)
      .subscribe((tasks: EpitopeTaskData[]) => {
        this.processes = tasks.map(task => ({
          ...task,
          elapsedTime: this.calculateElapsedTime(task.executionDate)
        }));
      });
  }

  private calculateElapsedTime(executionDate: Date, now: Date = new Date()): string {
    if (!executionDate) return 'N/A';

    const start = new Date(executionDate);
    const diffInMs = now.getTime() - start.getTime();

    if (diffInMs < 0) return 'Invalid date';

    const hours = Math.floor(diffInMs / (1000 * 60 * 60));
    const minutes = Math.floor((diffInMs % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diffInMs % (1000 * 60)) / 1000);

    let result = '';
    if (hours > 0) result += `${hours}h `;
    if (minutes > 0 || hours > 0) result += `${minutes}min `;
    result += `${seconds}s`;

    return result.trim();
  }
}
