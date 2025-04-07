import { Component } from "@angular/core";
import { Epitope } from "../../models/Epitope";
import { EpitopesService } from "../../services/epitopes/epitopes.service";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";
import { LoginService } from "../../services/login/login.service";

@Component({
  selector: "app-results",
  standalone: false,
  templateUrl: "./results.component.html",
  styleUrls: ["./results.component.scss"],
})
export class ResultsComponent {
  epitopes: Epitope[] = [];
  expandedEpitopeIndex: number | null = null;
  epitopeTaskData: EpitopeTaskData[] = [];
  filterText: string = "";
  filters: { [key: string]: string } = {};
  sortColumn: string = "";
  sortDirection: "asc" | "desc" = "asc";
  selectedEpitope: Epitope | null = null;
  selectedTask: Partial<EpitopeTaskData> = {};

  columns = [
    { key: "id", label: "ID" },
    { key: "epitope", label: "Epitope" },
    { key: "start", label: "Start" },
    { key: "end", label: "End" },
    { key: "length", label: "Length" },
    { key: "mwKDa", label: "MW (kDa)" },
    { key: "iP", label: "I.P" },
    { key: "hydropathy", label: "Hydropathy" },
    { key: "bepiPred3", label: "BepiPred3" },
    { key: "emini", label: "Emini" },
    { key: "kolaskar", label: "Kolaskar" },
    { key: "chouFosman", label: "Chou Fosman" },
    { key: "karplusSchulz", label: "Karplus Schulz" },
    { key: "parker", label: "Parker" },
  ];

  constructor(
    private epitopeService: EpitopesService,
    private loginService: LoginService
  ) {
    const userId = loginService.getUser()?.id;
    console.log("User ID:", userId);

    if (userId !== undefined) {
      epitopeService.getExecutedTasksByUserId(userId).subscribe((tasks) => {
        console.log("Tasks:", tasks);
        this.epitopeTaskData = tasks;
      });
    } else {
      console.error("User ID is undefined");
    }

    this.loadTable();
  }

  applyFilters() {
    const search = this.filterText.toLowerCase().trim();

    if (!search && !Array.isArray(this.selectedTask) && this.selectedTask?.epitopes) {
      this.epitopes = this.selectedTask.epitopes;
      return;
    }

    if (!Array.isArray(this.selectedTask) && this.selectedTask?.epitopes) {
      this.epitopes = this.selectedTask.epitopes.filter((epitope) =>
        Object.values(epitope).some((value) =>
          value?.toString().toLowerCase().includes(search)
        )
      );
    }
  }

  sort(columnKey: string) {
    if (this.sortColumn === columnKey) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortColumn = columnKey;
      this.sortDirection = "asc";
    }

    this.epitopes.sort((a, b) => {
      const valueA = (a as any)[columnKey];
      const valueB = (b as any)[columnKey];

      const aVal = isNaN(valueA) ? valueA : +valueA;
      const bVal = isNaN(valueB) ? valueB : +valueB;

      return aVal < bVal
        ? this.sortDirection === "asc"
          ? -1
          : 1
        : aVal > bVal
        ? this.sortDirection === "asc"
          ? 1
          : -1
        : 0;
    });
  }

  selectEpitope(epitope: Epitope | null) {
    this.epitopeService.selectEpitope(epitope);
  }

  loadTable() {
    this.epitopeService.selectedTask$.subscribe((task) => {
      this.selectedTask = task || {};

      if (!task?.epitopes || task.epitopes.length === 0) {
        this.epitopes = [];
        this.selectedEpitope = null;
        this.selectedTask = {};
        this.selectEpitope(null);
      } else {
        this.epitopes = task.epitopes;
      }
    });
  }

  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
