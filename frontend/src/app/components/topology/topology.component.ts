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
  columns: string[] = ["Protein ID", "N", "Method", "Epitope", "Threshold", "Avg Score", "Cover"];
  blasts: any[] = []
  blastColumns: string[] = ["Epitope ID", "Identity", "Cover", "Query subject", "Search subject"];

  columnMap: { [key: string]: keyof EpitopeTopology } = {
    Id: "id",
    N: "N",
    Method: "method",
    Threshold: "threshold",
    "Avg Score": "avgScore",
    Cover: "cover",
  };
  epitopeId?: number;
  proteinId?: string;
  database?: string;

  constructor(private epitopeService: EpitopesService) {
    this.loadTable();
  }

  ngOnInit() {
    this.loadTable();
  }

  getMaxTopologyLength(): number {
    if (!this.epitopeTopologies || this.epitopeTopologies.length === 0) {
      return 1; // Valor padrão quando não há dados
    }

    // Encontra o número máximo de caracteres válidos entre todas as linhas
    const maxLength = Math.max(...this.epitopeTopologies.map(row =>
      this.getValidTopologyChars(row.topologyData).length
    ));

    return maxLength > 0 ? maxLength : 1;
  }

  getValidTopologyChars(topologyData: string | undefined): string[] {
    if (!topologyData) {
      return [];
    }

    // Filtra caracteres vazios ou que são apenas whitespace
    return topologyData.split('').filter(char => char.trim() !== '');
  }

  loadTable() {
    this.epitopeService.selectedEpitope$.subscribe((epitope) => {

      // Verifica se o epítopo existe      
      if (epitope) {
        this.epitopeId = epitope.n;  // Define o epitopeId
        this.proteinId = epitope.epitopeId;
        this.database = epitope.blasts?.[0]?.database;

        // Carrega os epitopeTopologies
        this.epitopeTopologies = Array.isArray(epitope.epitopeTopologies)
          ? epitope.epitopeTopologies
          : epitope.epitopeTopologies ? [epitope.epitopeTopologies] : [];

        console.log("epitopeTopologies", this.epitopeTopologies);
        // Carrega os blasts
        this.blasts = Array.isArray(epitope.blasts)
          ? epitope.blasts
          : epitope.blasts ? [epitope.blasts] : [];
      } else {
        this.epitopeTopologies = [];  // Caso não exista epítopo, esvazia o array de topologies
        this.blasts = [];  // Caso não exista epítopo, esvazia o array de blasts
      }
    });
  }

  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
