import { Component } from "@angular/core";
import { EpitopeTopology } from "../../models/EpitopeTopology";
import { EpitopesService } from "../../services/epitopes/epitopes.service";

@Component({
  selector: "app-topology",
  standalone: false,
  templateUrl: "./topology.component.html",
  styleUrls: ["./topology.component.scss"],
})
export class TopologyComponent {
  epitopeTopologies: EpitopeTopology[] = [];
  expandedEpitopeIndex: number | null = null;
  epitopeId?: number;
  proteinId?: string;
  database?: string;

  blastColumns: string[] = ["Epitope ID", "Identity", "Cover", "Query subject", "Search subject"];
  blastsOriginal: any[] = [];
  blasts: any[] = [];
  filters: { [key: string]: string } = {};
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(private epitopeService: EpitopesService) { }

  ngOnInit() {
    this.loadTable();
  }

  loadTable() {
    this.epitopeService.selectedEpitope$.subscribe((epitope) => {
      if (epitope) {
        this.epitopeId = epitope.n;
        this.proteinId = epitope.epitopeId;
        this.database = epitope.blasts?.[0]?.database?.includes("iedb") ? "iedb" : undefined;

        this.epitopeTopologies = Array.isArray(epitope.epitopeTopologies)
          ? epitope.epitopeTopologies
          : epitope.epitopeTopologies ? [epitope.epitopeTopologies] : [];

        this.blastsOriginal = Array.isArray(epitope.blasts)
          ? epitope.blasts
          : epitope.blasts ? [epitope.blasts] : [];

        this.blastColumns.forEach(col => this.filters[col] = '');
        this.applyBlastFilters();
      } else {
        this.epitopeTopologies = [];
        this.blastsOriginal = [];
        this.blasts = [];
      }
    });
  }

  applyBlastFilters(): void {
    const filtered = this.blastsOriginal.filter(row => {
      for (const column of this.blastColumns) {
        const filterText = this.filters[column]?.trim().toLowerCase();
        if (!filterText) continue;

        const key = this.mapColumnKey(column);
        const cellValue = row[key] !== undefined && row[key] !== null
          ? String(row[key]).toLowerCase()
          : '';

        if (!cellValue.includes(filterText)) return false;
      }
      return true;
    });

    this.blasts = filtered;
    this.sortData();
  }

  sortBy(column: string): void {
    this.sortColumn = column;
    this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.sortData();
  }

  sortData(): void {
    if (!this.sortColumn) return;
    const key = this.mapColumnKey(this.sortColumn);
    this.blasts.sort((a, b) => {
      const valA = a[key] ?? '';
      const valB = b[key] ?? '';
      return this.sortDirection === 'asc'
        ? valA > valB ? 1 : -1
        : valA < valB ? 1 : -1;
    });
  }
  mapColumnKey(column: string): string {
    const map: { [key: string]: string } = {
      "Epitope ID": "sacc",
      "Identity": "pident",
      "Cover": "qcovs",
      "Query subject": "qseq",
      "Search subject": "sseq"
    };
    return map[column] || column;
  }

  getMaxTopologyLength(): number {
    if (!this.epitopeTopologies || this.epitopeTopologies.length === 0) {
      return 1;
    }

    const maxLength = Math.max(...this.epitopeTopologies.map(row =>
      this.getValidTopologyChars(row.topologyData).length
    ));

    return maxLength > 0 ? maxLength : 1;
  }

  getValidTopologyChars(topologyData: string | undefined): string[] {
    if (!topologyData) {
      return [];
    }

    return topologyData.split('').filter(char => char.trim() !== '');
  }

  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
