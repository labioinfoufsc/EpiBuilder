import { DatePipe } from "@angular/common";
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
        this.executedTasks = tasks;
      });
    } else {
      console.error("User ID is undefined");
    }
  }

  downloadTask(task: EpitopeTaskData): void {
    if (!task?.absolutePath) {
      console.error("Error: No file URL available");
      alert("Error: No file URL available");
      return;
    }

    this.epitopeService.downloadFile(task.id).subscribe({
      next: (response) => {
        const parts = task?.absolutePath?.split('/') || [];
        const fileName = parts.length >= 5 ? parts[4] : 'downloaded_file';

        saveAs(response.body!, fileName);
        console.log("File downloaded successfully:", fileName);
      },
      error: (error) => {
        console.error("Download failed:", error);
        alert("Error: Download failed. Please try again later.");
      },
    });
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
    console.log(task);
    this.selectedTask = task;
    this.epitopeService.selectTask(task);
  }
}