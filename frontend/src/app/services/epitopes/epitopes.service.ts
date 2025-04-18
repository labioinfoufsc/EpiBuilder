import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  BehaviorSubject,
  catchError,
  map,
  Observable,
  of,
  throwError,
} from "rxjs";
import { Epitope } from "../../models/Epitope";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";

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

  downloadFasta(fastaFile: File): Observable<APIResponse<File>> {
    if (!fastaFile.name.endsWith(".fasta")) {
      return of({
        success: false,
        message: "Invalid file type. Please upload a .fasta file.",
      });
    }

    return of({ success: true, message: fastaFile });
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

  getExecutedTasksByUserIdAndStatus(userId: number): Observable<EpitopeTaskData[]> {
    return this.http
      .get<EpitopeTaskData[]>(
        `${this.apiUrl}/tasks/user/${userId}/status`, // Note the /epitopes prefix
        { withCredentials: true }
      )
      .pipe(
        catchError((error) => {
          console.error("Full error:", error);
          return of([]);
        })
      );
  }

  submitForm(data: FormData): Observable<Epitope[]> {
    return this.http
      .post<APIResponse<Epitope[]>>(`${this.apiUrl}/tasks/new`, data)
      .pipe(
        map((response) => {
          if (!response?.success || !response.data) {
            throw new Error(
              typeof response?.message === "string"
                ? response.message
                : "Invalid response"
            );
          }
          return response.data as Epitope[];
        }),
        catchError((error: HttpErrorResponse) => {
          return throwError(() => error);
        })
      );
  }
}
