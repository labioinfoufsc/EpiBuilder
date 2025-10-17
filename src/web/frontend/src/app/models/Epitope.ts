import { Blast } from "./Blast";
import { EpitopeTopology } from "./EpitopeTopology";
import { Protein } from "./Protein";

export class Epitope {
  id?: string;
  avgCover?: string;
  blasts?: Blast[];
  n?: number;
  epitope?: string;
  epitopeTopologies?: EpitopeTopology[];
  start?: number;
  endEpitope?: number;
  nglyc?: string;
  nglycCount?: number;
  length?: number;
  molecularWeight?: number;
  isoelectricPoint?: number;
  hydropathy?: number;
  bepiPred3?: number;
  emini?: number;
  kolaskar?: number;
  chouFosman?: number;
  karplusSchulz?: number;
  parker?: number;
  protein?: Protein;
}
