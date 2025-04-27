import { DatePipe } from "@angular/common";
import { HttpResponse } from "@angular/common/http";
import { Component, ElementRef, ViewChild } from "@angular/core";
import { Modal } from "bootstrap";
import { saveAs } from 'file-saver';
import { APIResponse } from "../../models/APIResponse";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";
import { SuccessMessages } from "../../models/SuccessMessages";
import { EpitopesService } from "../../services/epitopes/epitopes.service";
import { LoginService } from "../../services/login/login.service";

@Component({
  selector: "app-last-executions",
  standalone: false,
  templateUrl: "./last-executions.component.html",
  styleUrls: ["./last-executions.component.scss"],
  providers: [DatePipe],
})
export class LastExecutionsComponent {
  executedTasks: EpitopeTaskData[] = [];
  selectedTask: EpitopeTaskData | null = null;
  taskToDelete: EpitopeTaskData | null = null;
  @ViewChild("deleteModal") deleteModal!: ElementRef;
  private deleteModalInstance!: Modal;
  alertMessage: string | null = null;
  alertType: "success" | "danger" | null = null;
  columns: string[] = [
    'Task',
    'Started At',
    'Completed At',
    'Duration',
    'Proteome size',
    'Status',
    'Actions'

  ];

  constructor(
    private epitopeService: EpitopesService,
    private loginService: LoginService,
    private datePipe: DatePipe
  ) {
    const userId = loginService.getUser()?.id;
    if (userId !== undefined) {
      epitopeService.getExecutedTasksByUserId(userId).subscribe((tasks) => {
        const filteredTasks = tasks.filter(task =>
          task.taskStatus?.status === 'COMPLETED' || task.taskStatus?.status === 'FINISHED'
        );

        this.executedTasks = filteredTasks;

      });

    }

  }

  downloadTask(task: EpitopeTaskData): void {
    if (!task || task.id === undefined) {
      console.error('ID da tarefa inválido');
      return;
    }

    this.epitopeService.downloadFile(task.id).subscribe({
      next: (response: any) => {
        if (response && response.blob) {
          const filename = response.filename || `task_${task.id}.zip`;
          const blob = new Blob([response.blob], { type: 'application/zip' });
          saveAs(blob, filename);
        } else {
          console.error('Erro ao baixar o arquivo');
        }
      }
    });
  }

  calculateExecutionTime(date: any, date2: any): string {
    if (!date || !date2) {
      return "N/A";
    }
    const startDate = new Date(date);
    const endDate = new Date(date2);
    const diff = endDate.getTime() - startDate.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    return `${hours}h ${minutes}m ${seconds}s`;
  }

  deleteTask(): void {
    const task: EpitopeTaskData | null = this.taskToDelete;

    if (!task) {
      this.showAlert("Error: No task selected for deletion", "danger");
      return;
    }

    if (task.id === undefined) {
      this.showAlert("Error: Task ID is undefined", "danger");
      return;
    }

    this.epitopeService.deleteTask(task.id).subscribe({
      next: (response: APIResponse<void>) => {
        const message = response.message || SuccessMessages.TaskDeleted;
        this.showAlert(message, "success");
        this.executedTasks = this.executedTasks.filter((t) => t.id !== task.id);
        this.cleanupAfterDelete();
      },
      error: (error) => {
        this.showAlert("Error deleting task: " + error.message, "danger");
        this.cleanupAfterDelete();
      },
    });
  }

  private cleanupAfterDelete(): void {
    this.taskToDelete = null;
    this.hideDeleteModal();
  }

  showAlert(message: string, type: "success" | "danger" | null) {
    this.alertMessage = message;
    this.alertType = type;

    setTimeout(() => {
      this.alertMessage = null;
    }, 5000);
  }

  showDeleteModal(task?: EpitopeTaskData) {
    if (this.deleteModal) {
      this.taskToDelete = task || null;
      this.deleteModalInstance = new Modal(this.deleteModal.nativeElement);
      this.deleteModalInstance.show();
    }
  }

  hideDeleteModal() {
    if (this.deleteModalInstance) {
      this.deleteModalInstance.hide();
    }
  }

  selectTask(task: EpitopeTaskData): void {
    this.selectedTask = task;
    this.epitopeService.selectTask(task);
  }
}