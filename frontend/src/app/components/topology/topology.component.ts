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
  columns: string[] = ["Id", "Method", "Threshold", "Avg Score", "Cover"];
  columnMap: { [key: string]: keyof EpitopeTopology } = {
    Id: "id",
    Method: "method",
    Threshold: "threshold",
    "Avg Score": "avgScore",
    Cover: "cover",
  };
  epitopeId?: string;

  constructor(private epitopeService: EpitopesService) {
    this.loadTable();
  }

  ngOnInit() {
    this.loadTable();
  }

  loadTable() {
    this.epitopeService.selectedEpitope$.subscribe((epitope) => {
      if (epitope) {
        this.epitopeId = epitope.id;
        this.epitopeTopologies = Array.isArray(epitope.epitopeTopologies)
          ? epitope.epitopeTopologies
          : epitope.epitopeTopologies
            ? [epitope.epitopeTopologies]
            : [];
      } else {
        this.epitopeTopologies = [];
      }
    });
  }

  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
