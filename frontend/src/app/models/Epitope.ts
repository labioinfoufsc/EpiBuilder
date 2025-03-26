import { EpitopeTopology } from "./EpitopeTopology";

export class Epitope {
  N?: number;
  id?: string;
  epitope?: string;
  topology?: EpitopeTopology;
  start?: number;
  end?: number;
  nGlyc?: string;
  nGlycCount?: number;
  length?: number;
  mwKDa?: number;
  iP?: number;
  hydropathy?: number;
  bepiPred3?: number;
  emini?: number;
  kolaskar?: number;
  chouFosman?: number;
  karplusSchulz?: number;
  parker?: number;
}
