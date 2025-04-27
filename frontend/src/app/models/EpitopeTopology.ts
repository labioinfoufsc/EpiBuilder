export class EpitopeTopology {
  N?: number;
  id?: string;
  method?: string;
  threshold?: number;
  avgScore?: number;
  cover?: number | null;
  topologyData?: string;

  constructor(data: Partial<EpitopeTopology>) {
    this.N = data.N;
    this.id = data.id;
    this.method = data.method;
    this.threshold = data.threshold;
    this.avgScore = data.avgScore;
    this.cover = data.cover ?? null;
    this.topologyData = data.topologyData;
  }
}
