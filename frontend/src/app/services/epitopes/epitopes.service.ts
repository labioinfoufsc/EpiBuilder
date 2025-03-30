import { Injectable } from "@angular/core";
import { Epitope } from "../../models/Epitope";
import { BehaviorSubject, catchError, map, Observable, of } from "rxjs";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";
import { HttpClient } from "@angular/common/http";

import { EpitopeTopology } from "../../models/EpitopeTopology";
import { ErrorMessages } from "../../models/ErrorMessages";
import { APIResponse } from "../../models/APIResponse";

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

  private mockEpitopes: Epitope[] = [
    {
      N: 1,
      id: "Q1ZXI9",
      epitope: "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNN",
      start: 0,
      end: 30,
      nGlyc: "N",
      nGlycCount: 0,
      length: 30,
      mwKDa: 3.44,
      iP: 5.52,
      hydropathy: -3.5,
      bepiPred3: 0.77,
      emini: 0.8,
      kolaskar: 0.28,
      chouFosman: 0.83,
      karplusSchulz: 0.62,
      parker: 1.25,
      topology: new EpitopeTopology({
        N: 1,
        id: "201",
        method: "BepiPred-3.0",
        threshold: 0.15,
        avgScore: 0.28,
        epitope: "FFFFFFFFFFFFFFFFFFFFFFF",
      }),
    },
  ];

  private mockEpitopeTasks: EpitopeTaskData[] = [
    {
      id: 1,
      runName: "Leish001",
      fasta: new File([""], "example.fasta", { type: "text/plain" }),
      action: "Prediction",
      bepipredThreshold: 0.5,
      minEpitopeLength: 8,
      maxEpitopeLength: 15,
      subcell: "Cytoplasm",
      interpro: "IPR001234",
      epitopeSearch: "HLA-A*02:01",
      optional: "None",
      date: new Date("2025-03-05T10:30:00Z"),
      epitopes: this.mockEpitopes,
    },
    {
      id: 2,
      runName: "Leish002",
      fasta: new File([""], "example.fasta", { type: "text/plain" }),
      action: "Prediction",
      bepipredThreshold: 0.5,
      minEpitopeLength: 8,
      maxEpitopeLength: 15,
      subcell: "Cytoplasm",
      interpro: "IPR001234",
      epitopeSearch: "HLA-A*02:01",
      optional: "None",
      date: new Date("2025-03-05T10:30:00Z"),
      epitopes: this.mockEpitopes,
    },
    {
      id: 3,
      runName: "Leish003",
      fasta: new File([""], "example.fasta", { type: "text/plain" }),
      action: "Prediction",
      bepipredThreshold: 0.5,
      minEpitopeLength: 8,
      maxEpitopeLength: 15,
      subcell: "Cytoplasm",
      interpro: "IPR001234",
      epitopeSearch: "HLA-A*02:01",
      optional: "None",
      date: new Date("2025-03-05T10:30:00Z"),
      epitopes: this.mockEpitopes,
    },
  ];

  constructor(private http: HttpClient) {}

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

  selectEpitope(epitope: Epitope) {
    this.selectedEpitopeSource.next(epitope);
  }

  selectTask(task: EpitopeTaskData) {
    this.selectedTaskSource.next(task);
  }

  getExecutedTasksByUser(): Observable<EpitopeTaskData[]> {
    return of(this.mockEpitopeTasks);
  }

  submitForm(data: EpitopeTaskData): Observable<Epitope[]> {
    this.epitopesSubject.next(this.mockEpitopes);
    return of(this.mockEpitopes);
  }
}
