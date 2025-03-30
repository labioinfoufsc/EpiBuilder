import { Component } from '@angular/core';
import { Epitope } from '../../models/Epitope';
import { EpitopesService } from '../../services/epitopes/epitopes.service';
import { EpitopeTaskData } from '../../models/EpitopeTaskData';

@Component({
  selector: 'app-results',
  standalone: false,
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css'],
})
export class ResultsComponent {
  epitopes: Epitope[] = [];
  expandedEpitopeIndex: number | null = null;
  epitopeTaskData: EpitopeTaskData[] = [];
  filterText: string = '';
  filters: { [key: string]: string } = {};
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  selectedEpitope: Epitope | null = null;
  selectedTask: EpitopeTaskData | null = null;

  columns = [
    { key: 'N', label: 'N' },
    { key: 'id', label: 'ID' },
    { key: 'epitope', label: 'Epitope' },
    { key: 'start', label: 'Start' },
    { key: 'end', label: 'End' },
    { key: 'length', label: 'Length' },
    { key: 'mwKDa', label: 'MW (kDa)' },
    { key: 'iP', label: 'I.P' },
    { key: 'hydropathy', label: 'Hydropathy' },
    { key: 'bepiPred3', label: 'BepiPred3' },
    { key: 'emini', label: 'Emini' },
    { key: 'kolaskar', label: 'Kolaskar' },
    { key: 'chouFosman', label: 'Chou Fosman' },
    { key: 'karplusSchulz', label: 'Karplus Schulz' },
    { key: 'parker', label: 'Parker' },
  ];

  constructor(private epitopeService: EpitopesService) {
    epitopeService.getExecutedTasksByUser().subscribe((tasks) => {
      this.epitopeTaskData = tasks;
    });

    this.epitopeTaskData.forEach((taskData) => {
      if (taskData.epitopes) {
        taskData.epitopes.forEach((epitope) => {
          this.epitopes.push(epitope);
        });
      }
    });

    this.loadTable();
  }

  /**
   * Filters epitopes based on the search text.
   */
  applyFilters() {
    const search = this.filterText.toLowerCase().trim();
    this.epitopes = this.epitopes.filter((epitope) => {
      return Object.values(epitope).some((value) =>
        value.toString().toLowerCase().includes(search)
      );
    });
  }

  /**
   * Sorts the epitopes based on the given column.
   * @param columnKey The key of the column to sort by.
   */
  sort(columnKey: string) {
    if (this.sortColumn === columnKey) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = columnKey;
      this.sortDirection = 'asc';
    }

    this.epitopes.sort((a, b) => {
      const valueA = (a as any)[columnKey];
      const valueB = (b as any)[columnKey];

      if (valueA < valueB) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }
      if (valueA > valueB) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }

  /**
   * Selects an epitope and updates the service.
   * @param epitope The epitope to select.
   */
  selectEpitope(epitope: Epitope) {
    this.epitopeService.selectEpitope(epitope);
  }

  /**
   * Loads the selected task data and logs it.
   */
  loadTable() {
    this.epitopeService.selectedTask$.subscribe((task) => {
      if (task) {
        console.log(task);
        this.selectedTask = task;
      }
    });
  }

  /**
   * Expands or collapses the details of a selected epitope.
   * @param index The index of the epitope to toggle.
   */
  toggleEpitope(index: number) {
    this.expandedEpitopeIndex =
      this.expandedEpitopeIndex === index ? null : index;
  }
}
