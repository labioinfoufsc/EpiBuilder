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
  n?: number;
  proteinId?: string;
  database?: string;

  blastColumns: string[] = ["Epitope ID", "DB", "Identity", "Cover", "Query subject", "Search subject"];
  blastsOriginal: any[] = [];
  blasts: any[] = [];
  filters: { [key: string]: string } = {};
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  tooltipMap: { [key: string]: string } = {
    BEPIPRED: 'Usage: B-cell epitope | Reference: BepiPred-3.0: Improved B-cell epitope prediction using protein language models',
    CHOU_FASMAN: 'Usage: Beta-Turn | Reference: Prediction of the Secondary Structure of Proteins from their Amino Acid Sequence',
    EMINI: 'Usage: Surface Accessibility | Reference: Induction of hepatitis A virus-neutralizing antibody by a virus-specific synthetic peptide',
    KARPLUS_SCHULZ: 'Usage: Chain flexibility | Reference: Prediction of chain flexibility in proteins',
    KOLASKAR: 'Usage: Antigenicity | Reference: A semi-empirical method for prediction of antigenic determinants on protein antigens',
    PARKER: 'Usage: Hydrophilicity | Reference: New hydrophilicity scale derived from high-performance liquid chromatography peptide retention data: correlation of predicted surface residues with antigenicity and x-ray-derived accessible sites',
    ALL_MATCHES: 'If the amino acid is above the cutoff point in all methods',
    N_GLYC: 'N-glycosylation sites',
    HYDROPATHY: 'Hydropathy + or -'
  };

  getTooltip(method?: string): string {
     if (!method) return '';
        return this.tooltipMap[method] ?? '';
  }

  getCellClass(char: string, i: number): string {
      if (i === 0) return 'text-dark fw-bold p-2';
      if (char === '.' || char === '-') return 'bg-danger text-danger fw-bold p-2';
        return 'bg-primary text-primary fw-bold p-2';
  }

  constructor(private epitopeService: EpitopesService) { }
  ngOnInit() {
    this.loadTable();
  }

  loadTable() {
    this.epitopeService.selectedEpitope$.subscribe((epitope) => {
      if (epitope) {
        this.n = epitope.n;
        this.proteinId = epitope?.protein?.proteinId;
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
