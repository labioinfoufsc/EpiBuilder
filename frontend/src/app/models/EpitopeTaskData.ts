import { Epitope } from "./Epitope";

/**
 * Represents the data required for submitting a new epitope search or analysis request.
 *
 * This model stores the necessary inputs for an epitope search or analysis request,
 * including file uploads, search parameters, and options for the execution. It is used 
 * to structure the data input by the user before initiating the analysis process.
 *
 * Properties:
 * - `id`: Optional unique identifier for the form data instance.
 * - `runName`: The name of the specific run or task associated with this epitope search.
 * - `fasta`: The FASTA file containing the sequences for epitope prediction.
 * - `action`: The specific action to be performed, such as prediction or analysis.
 * - `bepipredThreshold`: Threshold value for the BepiPred tool used in epitope prediction.
 * - `minEpitopeLength`: The minimum allowed length for detected epitopes.
 * - `maxEpitopeLength`: The maximum allowed length for detected epitopes.
 * - `subcell`: The subcellular location information, if available, for the epitope.
 * - `interpro`: The InterPro domain(s) associated with the epitope.
 * - `epitopeSearch`: The search term or parameters used for epitope identification.
 * - `results_folder`: The folder with results files.
 * - `optional`: Optional additional parameters for advanced configurations or customizations.
 *
 * This class is crucial for organizing and validating the user input in epitope-related 
 * analysis, ensuring that all necessary parameters are collected before processing the data.
 */
export class EpitopeTaskData {
  id?: number;
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
  date!: Date;
  /*results_folder!: File;*/
  epitopes?: Epitope[];
}
