import { ChangeDetectorRef, Component } from '@angular/core';
import { EpitopesService } from '../../services/epitopes/epitopes.service';
import { EpitopeTopology } from '../../models/EpitopeTopology';

@Component({
  selector: 'app-topology',
  standalone: false,
  templateUrl: './topology.component.html',
  styleUrls: ['./topology.component.css'],
})
export class TopologyComponent {
  topology: EpitopeTopology[] = [];
  expandedEpitopeIndex: number | null = null;
  columns: string[] = [
    'N',
    'Id',
    'Method',
    'Threshold',
    'Avg Score',
    'Cover',
    'Epitope'
  ];
  columnMap: { [key: string]: keyof EpitopeTopology } = {
    N: 'N',
    Id: 'id',
    Method: 'method',
    Threshold: 'threshold',
    'Avg Score': 'avgScore',
    Cover: 'cover',
    Epitope: 'epitope'
  };
  epitopeId?: string;

  constructor(
    private epitopeService: EpitopesService
  ) {
    this.loadTable();
  }

  ngOnInit() {
    this.loadTable();
  }

  loadTable() {
    this.epitopeService.selectedEpitope$.subscribe((epitope) => {
      if (epitope && epitope.topology) {
        this.epitopeId = epitope.id;
        this.topology = Array.isArray(epitope.topology)
          ? epitope.topology
          : [epitope.topology];

      }
    });
  }

  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
