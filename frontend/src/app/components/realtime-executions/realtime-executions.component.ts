import { Component } from '@angular/core';

@Component({
  selector: 'app-realtime-executions',
  standalone: false,
  templateUrl: './realtime-executions.component.html',
  styleUrls: ['./realtime-executions.component.css']
})
export class RealtimeExecutionsComponent {
  processes = [
    { pid: 1, taskName: 'Task A', processingTime: '5s', progress: 30, command: 'run task-a' },
    { pid: 2, taskName: 'Task B', processingTime: '10s', progress: 60, command: 'run task-b' },
    { pid: 3, taskName: 'Task C', processingTime: '3s', progress: 90, command: 'run task-c' }
  ];
}
