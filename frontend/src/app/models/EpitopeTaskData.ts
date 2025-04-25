import { Epitope } from "./Epitope";
import { TaskStatus } from "./TaskStatus"; // Ensure TaskStatus is defined in this module

export class EpitopeTaskData {
  id?: number;
  taskStatus?: TaskStatus;
  elapsedTime?: any;
  runName!: string;
  file!: File;
  absolutePath?: string;
  actionType?: string;
  bepipredThreshold!: number;
  minEpitopeLength!: number;
  maxEpitopeLength!: number;
  proteomeSize?: number;
  epitopeSearch!: string;
  executionDate!: Date;
  finishedDate?: Date;
  epitopes?: Epitope[];
  minIdentityCutoff?: number;
  maxIdentityCutoff?: number;
  wordSize?: number;
}

