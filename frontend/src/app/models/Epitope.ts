import { Blast } from "./Blast";
import { EpitopeTopology } from "./EpitopeTopology";

export class Epitope {
  id?: string;
  blasts?: Blast[];
  n?: number;
  epitopeId?: string;
  epitope?: string;
  epitopeTopologies?: EpitopeTopology[];
  start?: number;
  end?: number;
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
}
