import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Modal } from "bootstrap";
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
  columns: string[] = ['PID', 'Task name', 'Started At', 'Elapsed Time', 'Status', 'Actions'];
  userId: number | undefined;
  taskListChangedSubscription: Subscription | undefined;
  logText: string = '';
  modalVisible: boolean = false;
  currentProcess: EpitopeTaskData | undefined;

  private tableUpdateInterval = 30000;
  private logUpdateInterval = 5000;
  private tableUpdateTimer: any;
  private logUpdateTimer: any;
  private elapsedTimeUpdateTimer: any;
  private isUpdatingLog = false;

  @ViewChild('logContent') private logContentRef!: ElementRef;

  @ViewChild('stopModal') stopModalRef!: ElementRef;

  selectedProcessToStop: EpitopeTaskData | undefined;

  constructor(
    private epitopesService: EpitopesService,
    private loginService: LoginService
  ) { }

  ngOnInit(): void {
    document.addEventListener('keydown', this.handleEscape, true);
    this.userId = this.loginService.getUser()?.id;
    if (this.userId !== undefined) {
      this.loadTasks();
      this.startTableUpdates();
      this.startElapsedTimeUpdates();

      this.taskListChangedSubscription = this.epitopesService.taskListChanged$.subscribe(() => {
        this.loadTasks();
      });
    } else {
      console.error("User ID is undefined");
    }
  }

  ngOnDestroy(): void {
    document.removeEventListener('keydown', this.handleEscape, true);
    this.cleanUpIntervals();
    this.taskListChangedSubscription?.unsubscribe();
  }

  private handleEscape = (event: KeyboardEvent): void => {
    if (event.key === 'Escape') {
      this.closeModal();
    }
  };

  private scrollToBottom(): void {
    const element = this.logContentRef?.nativeElement;
    if (element) {
      element.scrollTop = element.scrollHeight;
    }
  }

  /**
 * Opens the stop confirmation modal.
 */
  stopProcess(process: EpitopeTaskData): void {
    this.selectedProcessToStop = process;
    const modalEl = this.stopModalRef.nativeElement;
    const modal = new Modal(modalEl, { backdrop: false });
    modal.show();
  }

  /**
   * Closes the stop modal.
   */
  hideStopModal(): void {
    const modalEl = this.stopModalRef.nativeElement;
    const modal = Modal.getInstance(modalEl);
    modal?.hide();
    this.selectedProcessToStop = undefined;
  }

  /**
   * Confirms and executes the stop action.
   */
  confirmStop(): void {
    if (!this.selectedProcessToStop) return;

    const processToStop = this.selectedProcessToStop;

    this.hideStopModal();

    if (!processToStop.taskStatus) {
      (processToStop as any).taskStatus = { status: 'STOPPING' };
    } else {
      processToStop.taskStatus.status = 'STOPPING';
    }

    this.epitopesService.stopTask(processToStop.id).subscribe({
      next: () => {
        console.log(`Process ${processToStop.id} stopped successfully.`);
        this.loadTasks();
      },
      error: (err: any) => {
        console.error(`Failed to stop process ${processToStop.id}:`, err);
        this.loadTasks();
      }
    });
  }


  /**
   * Closes the stop confirmation modal.
   */
  closeStopModal(): void {
    this.selectedProcessToStop = undefined;
  }

  public onOverlayClick(event: MouseEvent): void {
    this.closeModal();
  }

  private startTableUpdates(): void {
    if (this.tableUpdateTimer) return;
    this.tableUpdateTimer = setInterval(() => {
      this.loadTasks();
    }, this.tableUpdateInterval);
  }

  private startElapsedTimeUpdates(): void {
    if (this.elapsedTimeUpdateTimer) return;
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
    if (this.logUpdateTimer || this.currentProcess?.status === 'COMPLETED') return;
    this.logUpdateTimer = setInterval(() => {
      this.updateLogContent();
    }, this.logUpdateInterval);
  }

  private cleanUpTableInterval(): void {
    clearInterval(this.tableUpdateTimer);
    this.tableUpdateTimer = undefined;
  }

  private cleanUpLogInterval(): void {
    clearInterval(this.logUpdateTimer);
    this.logUpdateTimer = undefined;
  }

  private cleanUpElapsedTimeInterval(): void {
    clearInterval(this.elapsedTimeUpdateTimer);
    this.elapsedTimeUpdateTimer = undefined;
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
    this.loadTasks();
  }

  private updateLogContent(): void {
    if (!this.currentProcess?.id || this.isUpdatingLog) return;

    this.isUpdatingLog = true;

    this.epitopesService.getTaskLog(this.currentProcess.id).subscribe({
      next: (logBlob: Blob) => {
        const reader = new FileReader();
        reader.onload = () => {
          const logText = reader.result as string;
          this.logText = logText;
          setTimeout(() => this.scrollToBottom(), 0);

          if (
            this.currentProcess &&
            logText.includes('Pipeline finished') &&
            this.currentProcess.status !== 'COMPLETED'
          ) {
            const taskId = this.currentProcess.id;
            if (typeof taskId === 'number') {
              this.currentProcess.status = 'COMPLETED';

              this.epitopesService.markTaskAsCompleted(taskId).subscribe({
                next: () => {
                  this.cleanUpLogInterval();
                  this.loadTasks(); 
                  this.startTableUpdates();
                  this.startElapsedTimeUpdates();
                },
                error: (err) => {
                  console.error('Error while notifying backend:', err);
                }
              });
            }
          }

          this.isUpdatingLog = false;
        };
        reader.onerror = (error) => {
          this.logText = 'Error loading log: ' + error;
          this.isUpdatingLog = false;
        };
        reader.readAsText(logBlob);
      },
      error: (err: { error: any; message: any }) => {
        this.logText = 'Error loading log: ' + (err.error || err.message);
        this.isUpdatingLog = false;
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
