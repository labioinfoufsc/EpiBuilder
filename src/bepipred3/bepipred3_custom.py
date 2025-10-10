### IMPORTS AND STATIC PATHS ###
import sys
from pathlib import Path
import argparse
from bp3 import bepipred3

WORK_DIR = Path( Path(__file__).parent.resolve() )

### COMMAND LINE ARGUMENTS ###
parser = argparse.ArgumentParser("Make B-cell epitope predictions from fasta file.")
parser.add_argument("-i", required=True, action="store", dest="fasta_file", type=Path, help="Fasta file contianing antigens")
parser.add_argument("-o", required=True, action="store", dest="out_dir", type=Path, help="Output directory to store B-cell epitope predictions.")
parser.add_argument("-add_seq_len", action="store_true", dest="add_seq_len", help="Add sequence lengths to esm-encodings. Default is false. This option is set to true for the web server.")
parser.add_argument("-esm_dir", action="store", default= WORK_DIR / "esm_encodings", dest="esm_dir", type=Path, help="Directory to save esm encodings to. Default is current working directory.")
parser.add_argument("-top", action="store", default=0.2, type=float, dest="top_cands", help="Top percentage of epitope residues Default is top 20 pct.")
parser.add_argument("-rolling_window_size", default=9, type=int, dest="rolling_window_size", help="Window size to use for rolling average on B-cell epitope probability scores. Default is 9.")

args = parser.parse_args()
fasta_file = args.fasta_file
out_dir = args.out_dir
add_seq_len = args.add_seq_len
esm_dir = args.esm_dir
top_cands = args.top_cands
rolling_window_size = args.rolling_window_size

#on webservices, we have the esm2 model stored locally. To work you need both esm2_t33_650M_UR50D.pt and the esm2_t33_650M_UR50D-contact-regression.pt stored in same directory
MyAntigens = bepipred3.Antigens(fasta_file, esm_dir, add_seq_len=add_seq_len, run_esm_model_local=str(WORK_DIR / "models" / "esm2_t33_650M_UR50D.pt") )

#MyAntigens = bepipred3.Antigens(fasta_file, esm_dir, add_seq_len=add_seq_len)
MyBP3EnsemblePredict = bepipred3.BP3EnsemblePredict(MyAntigens, rolling_window_size=rolling_window_size, top_pred_pct = top_cands)
MyBP3EnsemblePredict.run_bp3_ensemble()
MyBP3EnsemblePredict.create_csvfile(out_dir)
