import { Component } from '@angular/core';
import { EpitopesService } from '../../services/epitopes/epitopes.service';
import { EpitopeTaskData } from '../../models/EpitopeTaskData';
import { DatePipe } from '@angular/common';
import { SuccessMessages } from '../../models/SuccessMessages';
import { APIResponse } from '../../models/APIResponse';

@Component({
  selector: 'app-last-executions',
  standalone: false,
  templateUrl: './last-executions.component.html',
  styleUrls: ['./last-executions.component.css'],
  providers: [DatePipe],
})
export class LastExecutionsComponent {
  executedTasks: EpitopeTaskData[] = [];

  constructor(
    private epitopeService: EpitopesService,
    private datePipe: DatePipe
  ) {
    this.epitopeService.getExecutedTasksByUser().subscribe((tasks) => {
      this.executedTasks = tasks;
    });
  }

  downloadTask(task: EpitopeTaskData): void {
    if (!task?.fasta) {
      console.error('Error: No file URL available');
      alert('Error: No file URL available');
      return;
    }

    if (task.fasta) {
      this.epitopeService.downloadFasta(task.fasta).subscribe({
        next: (response: APIResponse<Blob>) => {
          if (response.success && response.message) {
            saveAs(response.message, 'file.fasta');
            console.log('File downloaded successfully');
          } else {
            console.error('Error: ' + response.message);
            alert('Error: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Download failed:', error);
          alert('Error: Download failed. Please try again later.');
        },
      });
    }
  }

  deleteTask(task: EpitopeTaskData): void {
    if (task.id) {
      this.epitopeService
        .deleteTask(task.id)
        .subscribe((response: APIResponse<void>) => {
          if (!response.success) {
            alert(`Error: ${response.message}`);
          } else {
            alert(SuccessMessages.TaskDeleted);
            this.executedTasks = this.executedTasks.filter(
              (task) => task.id !== task.id
            );
          }
        });
    }
  }

  selectTask(task: EpitopeTaskData): void {
    this.epitopeService.selectTask(task);
  }
}
function saveAs(message: string | Blob, arg1: string) {
  throw new Error('Function not implemented.');
}

