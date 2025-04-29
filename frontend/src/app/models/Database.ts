export class Database {
  id?: number;
  alias?: string;
  fileName!: string;
  absolutePath!: string;
  date?: Date;
  sourceType?: string;
  dbFile?: File;
}
