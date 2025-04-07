import { EpitopeTopology } from "./EpitopeTopology";

export class Epitope {
  id?: string;
  epitope?: string;
  epitopeTopologies?: EpitopeTopology[];
  start?: number;
  end?: number;
  nGlyc?: string;
  nGlycCount?: number;
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
