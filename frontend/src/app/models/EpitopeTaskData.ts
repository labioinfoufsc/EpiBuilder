import { Epitope } from "./Epitope";
import { TaskStatus } from "./TaskStatus"; // Ensure TaskStatus is defined in this module

export class EpitopeTaskData {
  id?: number;
  taskStatus?: TaskStatus;
  runName!: string;
  fasta!: File;
  action!: string;
  bepipredThreshold!: number;
  minEpitopeLength!: number;
  maxEpitopeLength!: number;
  subcell!: string;
  interpro!: string;
  epitopeSearch!: string;
  optional!: string;
  executionDate!: Date;
  finishedDate?: Date;
  /*results_folder!: File;*/
  epitopes?: Epitope[];
}
