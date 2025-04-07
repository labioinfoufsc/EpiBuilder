export class TaskStatus {
  taskUUID: number;
  status: string;
  progress: number;

  constructor(pid: number, status: string, progress: number) {
    this.taskUUID = pid;
    this.status = status;
    this.progress = progress;
  }
}