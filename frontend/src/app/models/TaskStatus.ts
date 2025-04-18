export class TaskStatus {
  pid: number;
  status: string;
  progress: number;

  constructor(pid: number, status: string, progress: number) {
    this.pid = pid;
    this.status = status;
    this.progress = progress;
  }
}