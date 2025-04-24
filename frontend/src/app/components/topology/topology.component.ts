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

  loadTable() {
    this.epitopeService.selectedEpitope$.subscribe((epitope) => {
      if (epitope) {
        this.epitopeId = epitope.n;  // Define o epitopeId
        this.proteinId = epitope.epitopeId;
        this.database = epitope.blasts?.[0]?.database;

        // Carrega os epitopeTopologies
        this.epitopeTopologies = Array.isArray(epitope.epitopeTopologies)
          ? epitope.epitopeTopologies
          : epitope.epitopeTopologies ? [epitope.epitopeTopologies] : [];

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
