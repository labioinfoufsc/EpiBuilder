import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Database } from "../../models/Database";
import { Epitope } from "../../models/Epitope";
import { EpitopeTaskData } from "../../models/EpitopeTaskData";
import { EpitopesService } from "../../services/epitopes/epitopes.service";
import { LoginService } from "../../services/login/login.service";

@Component({
  selector: "app-results",
  standalone: false,
  templateUrl: "./results.component.html",
  styleUrls: ["./results.component.scss"],
})
export class ResultsComponent implements OnInit {
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
    { key: "n", label: "N" },
    { key: "protein.proteinId", label: "Protein ID" },
    { key: "protein.description", label: "Description" },
    { key: "protein.localization", label: "Localization" },
    { key: "epitope", label: "Epitope" },
    { key: "avgCover", label: "EpiBuilder Score" },
    { key: "start", label: "Start" },
    { key: "endEpitope", label: "End" },
    { key: "length", label: "Length" },
    { key: "molecularWeight", label: "kDa" },
    { key: "isoelectricPoint", label: "pH(I)" },
    { key: "nglyc", label: "nGlyc" },
    { key: "hydropathy", label: "Hydropathy" },
    { key: "bepiPred3", label: "BepiPred3" },
    { key: "kolaskar", label: "Antigenicity" },
    { key: "blasts", label: "Search Hit" },
  ];


  constructor(
    private epitopeService: EpitopesService,
    private loginService: LoginService
  ) {
    this.getData();
  }

  getData() {
    const userId = this.loginService.getUser()?.id;

    if (userId !== undefined) {
      this.epitopeService.getExecutedTasksByUserId(userId).subscribe((tasks) => {
        this.epitopeTaskData = tasks;
      });
    } else {
      console.error("User ID is undefined");
    }

    this.loadTable();
  }

  ngOnInit(): void {
    this.resetState();
    this.getData();
  }

  resetState(): void {
    this.epitopes = [];
    this.expandedEpitopeIndex = null;
    this.epitopeTaskData = [];
    this.filterText = "";
    this.filters = {};
    this.sortColumn = "";
    this.sortDirection = "asc";
    this.selectedEpitope = null;
    this.selectedTask = {};
  }

  applyFilters() {
    const search = this.filterText.toLowerCase().trim();

    if (
      !search &&
      !Array.isArray(this.selectedTask) &&
      this.selectedTask?.epitopes
    ) {
      this.epitopes = this.selectedTask.epitopes;
      return;
    }

    if (!Array.isArray(this.selectedTask) && this.selectedTask?.epitopes) {
      this.epitopes = this.selectedTask.epitopes.filter((epitope) => {
        const matchInEpitope = Object.entries(epitope).some(([key, value]) => {
          if (typeof value === 'string' || typeof value === 'number') {
            return value.toString().toLowerCase().includes(search);
          }
          return false;
        });

        const matchInProtein = epitope.protein &&
          Object.entries(epitope.protein).some(([key, value]) => {
            if (typeof value === 'string') {
              return value.toLowerCase().includes(search);
            }
            return false;
          });

        return matchInEpitope || matchInProtein;
      });
    }
  }



  sort(columnKey: string) {
    if (this.sortColumn === columnKey) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortColumn = columnKey;
      this.sortDirection = "asc";
    }

    const getNestedValue = (obj: any, path: string): any => {
      return path.split('.').reduce((acc, part) => acc?.[part], obj);
    };

    this.epitopes.sort((a, b) => {
      const valueA = getNestedValue(a, columnKey);
      const valueB = getNestedValue(b, columnKey);

      const aVal = isNaN(valueA) || valueA === null ? valueA : +valueA;
      const bVal = isNaN(valueB) || valueB === null ? valueB : +valueB;

      return aVal < bVal
        ? this.sortDirection === "asc" ? -1 : 1
        : aVal > bVal
          ? this.sortDirection === "asc" ? 1 : -1
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
