import { HttpClient, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  BehaviorSubject,
  catchError,
  Observable,
  of
} from "rxjs";
import { Epitope } from "../../models/Epitope";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";

import { Subject } from 'rxjs';
import { APIResponse } from "../../models/APIResponse";
import { ErrorMessages } from "../../models/ErrorMessages";

@Injectable({
  providedIn: "root",
})
export class EpitopesService {
  private epitopesSubject = new BehaviorSubject<Epitope[]>([]);
  epitopes$ = this.epitopesSubject.asObservable();

  private selectedEpitopeSource = new BehaviorSubject<Epitope | null>(null);
  selectedEpitope$ = this.selectedEpitopeSource.asObservable();

  private selectedTaskSource = new BehaviorSubject<EpitopeTaskData | null>(
    null
  );
  selectedTask$ = this.selectedTaskSource.asObservable();

  private taskListChanged = new Subject<void>();
  taskListChanged$ = this.taskListChanged.asObservable();

  notifyTaskListChanged(): void {
    this.taskListChanged.next();
  }

  private environment: string = "http://localhost:8080";
  private apiUrl = `${this.environment}/epitopes`;

  constructor(private http: HttpClient) { }

  getTaskStatus(taskId: number): Observable<APIResponse<EpitopeTaskData>> {
    return this.http.get<APIResponse<EpitopeTaskData>>(
      `${this.apiUrl}/tasks/status/${taskId}`,
      { withCredentials: true }
    );
  }

  deleteTask(taskId: number): Observable<APIResponse<void>> {
    return this.http
      .delete<APIResponse<void>>(`${this.apiUrl}/tasks/${taskId}`)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.message || ErrorMessages.TaskNotFound;
          return of({ success: false, message: errorMessage });
        })
      );
  }

  downloadFile(taskId: number | undefined): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.apiUrl}/task/${taskId}/download`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  selectEpitope(epitope: Epitope | null) {
    this.selectedEpitopeSource.next(epitope);
  }

  selectTask(task: EpitopeTaskData) {
    this.selectedTaskSource.next(task);
  }

  getExecutedTasksByUserId(userId: number): Observable<EpitopeTaskData[]> {
    return this.http
      .get<EpitopeTaskData[]>(
        `${this.apiUrl}/tasks/user/${userId}`, // Note the /epitopes prefix
        { withCredentials: true }
      )
      .pipe(
        catchError((error) => {
          console.error("Full error:", error);
          return of([]);
        })
      );
  }

  getExecutedTasksByUserIdAndStatus(userId: number | undefined): Observable<EpitopeTaskData[]> {
    return this.http
      .get<EpitopeTaskData[]>(
        `${this.apiUrl}/tasks/user/${userId}/status`,
        { withCredentials: true }
      )
      .pipe(
        catchError((error) => {
          console.error("Full error:", error);
          return of([]);
        })
      );
  }

  submitForm(data: FormData): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/tasks/new`, data);
  }
}
