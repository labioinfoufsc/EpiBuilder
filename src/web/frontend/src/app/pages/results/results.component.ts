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
  epitopesOriginal: Epitope[] = [];
  expandedEpitopeIndex: number | null = null;
  epitopeTaskData: EpitopeTaskData[] = [];
  filterText: string = "";
  filters: { [key: string]: string } = {};
  sortColumn: string = "";
  sortDirection: "asc" | "desc" = "asc";
  selectedEpitope: Epitope | null = null;
  selectedTask: Partial<EpitopeTaskData> = {};

  columns: { key: string, label: string }[] = [];
  dynamicDbs: string[] = [];
  blastCounts: { [epitopeId: number]: { [dbName: string]: number } } = {}; 
  hasDescription: boolean = true;
  hasLocalization: boolean = true;
  generateColumns() {
    this.columns = [
    { key: 'n', label: 'N' },
    { key: 'protein.proteinId', label: 'Protein ID' },
    { key: 'protein.description', label: 'Description' },
    { key: 'protein.localization', label: 'Localization' },
    { key: 'epitope', label: 'Epitope' },
    { key: 'avgCover', label: 'Avg Cover' },
    { key: 'start', label: 'Start' },
    { key: 'endEpitope', label: 'End' },
    { key: 'length', label: 'Length' },
    { key: 'molecularWeight', label: 'Mol. Weight' },
    { key: 'isoelectricPoint', label: 'Isoelectric Point' },
    { key: 'nglyc', label: 'N-Glyc' },
    { key: 'hydropathy', label: 'Hydropathy' },
    { key: 'bepiPred3', label: 'BepiPred3' },
    { key: 'kolaskar', label: 'Kolaskar' }
    ];

    this.hasDescription = this.epitopes.some(
      e => e.protein?.description && e.protein.description !== '-'
    );
    if (!this.hasDescription) {
      this.columns = this.columns.filter(c => c.key !== 'protein.description');
    }
    

    this.hasLocalization = this.epitopes.some(
      e => e.protein?.localization && e.protein.localization !== '-'
    );
    if (!this.hasLocalization) {
      this.columns = this.columns.filter(c => c.key !== 'protein.localization');
    }

    const dbSet = new Set<string>();
      this.epitopes.forEach(e => {
        e.blasts?.forEach(b => {
         if (b.db) dbSet.add(b.db);
        });
    });

    this.dynamicDbs = Array.from(dbSet);
    this.epitopes.forEach(e => {
      this.dynamicDbs.forEach(dbName => {
      const count = e.blasts?.filter(b => b.db === dbName).length || 0;
      (e as any)[`blastCount_${dbName}`] = count;
      });
    });
  }

  constructor(
    private epitopeService: EpitopesService,
    private loginService: LoginService
  ) {
    this.getData();
  }

  getNestedValue = (obj: any, path: string): any => {
    return path.split('.').reduce((acc, part) => acc?.[part], obj);
  };

  getBlastCount(epitope: Epitope, dbName: string): number {
  return (epitope as any)[`blastCount_${dbName}`] || 0;
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
    this.generateColumns();
    this.getData();
  }

  resetState(): void {
    this.epitopes = [];
    this.columns = [];
    this.expandedEpitopeIndex = null;
    this.epitopeTaskData = [];
    this.filterText = "";
    this.filters = {};
    this.sortColumn = "";
    this.sortDirection = "asc";
    this.selectedEpitope = null;
    this.selectedTask = {};
  }

  applyColumnFilters() {
    let filtered = this.epitopesOriginal;

    for (const col of this.columns) {
      const filterText = this.filters[col.key]?.trim().toLowerCase();
      if (filterText) {
        filtered = filtered.filter(epitope => {
          const cellValue = this.getNestedValue(epitope, col.key);
          return String(cellValue ?? '').toLowerCase().includes(filterText);
        });
      }
    }

    for (const dbName of this.dynamicDbs) {
      const filterText = this.filters[dbName]?.trim().toLowerCase();
      if (filterText) {
        filtered = filtered.filter(epitope => {
          const cellValue = this.getBlastCount(epitope, dbName);
          return String(cellValue ?? '').toLowerCase().includes(filterText);
        });
      }
    }

    this.epitopes = filtered;
    this.sortData();
  }

  sort(columnKey: string) {
    if (this.sortColumn === columnKey) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortColumn = columnKey;
      this.sortDirection = "asc";
    }
    this.sortData();
  }

  sortData() {
    if (!this.sortColumn) return;

    this.epitopes.sort((a, b) => {
      const valueA = this.getNestedValue(a, this.sortColumn);
      const valueB = this.getNestedValue(b, this.sortColumn);

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
        this.epitopesOriginal = [];
        this.selectedEpitope = null;
        this.selectedTask = {};
        this.selectEpitope(null);
      } else {
        this.epitopes = [...task.epitopes];
        this.epitopesOriginal = [...task.epitopes];
      }
      this.generateColumns();
      this.initializeFilters();
    });
  }

  initializeFilters(): void {
    this.filters = {};
    this.columns.forEach(col => this.filters[col.key] = '');
    this.dynamicDbs.forEach(dbName => this.filters[dbName] = '');
  }

  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
