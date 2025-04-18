import { Epitope } from "./Epitope";
import { TaskStatus } from "./TaskStatus"; // Ensure TaskStatus is defined in this module

export class EpitopeTaskData {
  id?: number;
  taskStatus?: TaskStatus;
  runName!: string;
  file!: File;
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
  epitopes?: Epitope[];
  algpredThreshold?: number;
  algPredPredictionModelType?: string;
  algPredDisplayMode?: string;
}
