export class EpitopeTopology {
  N?: number;
  id?: string;
  method?: string;
  threshold?: number;
  avgScore?: number;
  cover?: number | null;
  epitope?: string;

  constructor(data: Partial<EpitopeTopology>) {
    this.N = data.N;
    this.id = data.id;
    this.method = data.method;
    this.threshold = data.threshold;
    this.avgScore = data.avgScore;
    this.cover = data.cover ?? null; 
    this.epitope = data.epitope;
  }
}
