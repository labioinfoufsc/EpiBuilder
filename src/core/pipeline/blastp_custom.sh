#!/bin/bash
# ============================================
# blastp_custom.sh
# ============================================
# Uso:
#   bash blastp_custom.sh <query_fasta> <db> <blast_task> <word_size> <identity> <coverage> <output_file>
# ============================================

set -euo pipefail

QUERY_FASTA="$1"
DB="$2"
BLAST_TASK="$3"
WORD_SIZE="$4"
IDENTITY="$5"
COVERAGE="$6"
OUTPUT="$7"

echo "[INFO] Running BLAST with filters..."
echo "       blastp: system default"
echo "       query:  $QUERY_FASTA"
echo "       db:     $DB"
echo "       output: $OUTPUT"
echo "       identity >= $IDENTITY"
echo "       coverage >= $COVERAGE"
echo

# Executa o BLASTP e filtra com awk
blastp \
  -query "$QUERY_FASTA" \
  -db "$DB" \
  -outfmt "6 qacc sacc pident qcovs qseq sseq" \
  -task "$BLAST_TASK" \
  -word_size "$WORD_SIZE" \
| awk -v id="$IDENTITY" -v cov="$COVERAGE" '$3 >= id && $4 >= cov' > "$OUTPUT"

echo "[INFO] Finished! Filtered results saved to: $OUTPUT"
