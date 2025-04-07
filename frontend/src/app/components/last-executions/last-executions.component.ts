import { Component, ElementRef, ViewChild } from "@angular/core";
import { EpitopesService } from "../../services/epitopes/epitopes.service";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";
import { DatePipe } from "@angular/common";
import { SuccessMessages } from "../../models/SuccessMessages";
import { APIResponse } from "../../models/APIResponse";
import { LoginService } from "../../services/login/login.service";
import { Modal } from "bootstrap";

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
    loginService: LoginService,
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
    if (!task?.fasta) {
      console.error("Error: No file URL available");
      alert("Error: No file URL available");
      return;
    }

    if (task.fasta) {
      this.epitopeService.downloadFasta(task.fasta).subscribe({
        next: (response: APIResponse<Blob>) => {
          if (response.success && response.message) {
            saveAs(response.message, "file.fasta");
            console.log("File downloaded successfully");
          } else {
            console.error("Error: " + response.message);
            alert("Error: " + response.message);
          }
        },
        error: (error) => {
          console.error("Download failed:", error);
          alert("Error: Download failed. Please try again later.");
        },
      });
    }
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
function saveAs(message: string | Blob, arg1: string) {
  throw new Error("Function not implemented.");
}
