import sys
from Bio import SeqIO

# Valid amino acids (single-letter code)
VALID_AMINOACIDS = set("ACDEFGHIKLMNPQRSTVWY")

def is_valid_sequence(seq):
    return all(residue in VALID_AMINOACIDS for residue in seq)

def validate_fasta(input_file):
    valid_records = []
    invalid_records = []

    for record in SeqIO.parse(input_file, "fasta"):
        seq_str = str(record.seq).upper()
        if is_valid_sequence(seq_str):
            valid_records.append(record)
        else:
            invalid_records.append(record)

    if valid_records:
        SeqIO.write(valid_records, "proteins_valid.fasta", "fasta")

    if invalid_records:
        SeqIO.write(invalid_records, "proteins_invalid.fasta", "fasta")

    if not invalid_records:
        print("All protein sequences are valid. File 'proteins_valid.fasta' has been created.")
    else:
        print(f"{len(invalid_records)} invalid protein sequence(s) found.")
        print("Files 'proteins_valid.fasta' and 'proteins_invalid.fasta' have been created.")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python validate_fasta.py <input_fasta>")
        sys.exit(1)

    input_fasta = sys.argv[1]
    validate_fasta(input_fasta)
