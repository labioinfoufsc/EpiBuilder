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

BASE_DIR="/tmp/epibuilder/blastp"

# Nome do arquivo e diret  rio original
FILENAME_QUERY=$(basename "$QUERY_FASTA")
FILENAME_DB=$(basename "$DB")
FILENAME_OUTPUT=$(basename "$OUTPUT")

RANDOM_ID=$(cat /proc/sys/kernel/random/uuid)
TMP_DIR="$BASE_DIR/$RANDOM_ID"

QUERY_DOCKER="$TMP_DIR/$FILENAME_QUERY"
DB_DOCKER="$TMP_DIR/$FILENAME_DB"
OUT_DOCKER="$TMP_DIR/$FILENAME_OUTPUT"

# Cria o diret  rio tempor  rio local e copia o arquivo para l
mkdir -p "$TMP_DIR"
cp "$QUERY_FASTA" "$QUERY_DOCKER"
cp "$DB" "$DB_DOCKER"


echo "[INFO] Copied $QUERY_FASTA and $DB to $QUERY_DOCKER and $DB_DOCKER"

EPIBUILDER_VOLUME="${EPIBUILDER_VOLUME:-/tmp/epibuilder}"

docker run --rm \
  -v "$EPIBUILDER_VOLUME:/tmp/epibuilder" \
  -w /tmp/epibuilder \
  staphb/blast:2.17.0 \
  bash -c "
    makeblastdb \
      -in '$DB_DOCKER' \
      -dbtype prot \
      -out '$DB_DOCKER'
  "

docker run --rm \
  -v "$EPIBUILDER_VOLUME:/tmp/epibuilder" \
  staphb/blast:2.17.0 \
  bash -c "
    blastp \
      -query '$QUERY_DOCKER' \
      -db '$DB_DOCKER' \
      -outfmt '6 qacc sacc pident qcovs qseq sseq' \
      -task '$BLAST_TASK' \
      -word_size '$WORD_SIZE' > '$OUT_DOCKER.tmp' && \
    awk -v id='$IDENTITY' -v cov='$COVERAGE' '\$3 >= id && \$4 >= cov' '$OUT_DOCKER.tmp' > '$OUT_DOCKER'  "


echo -e "qacc\tsacc\tpident\tqcovs\tqseq\tsseq" | cat - "$OUT_DOCKER" > "$OUT_DOCKER.tmpfile" && mv "$OUT_DOCKER.tmpfile" "$OUT_DOCKER"
echo -e "qacc\tsacc\tpident\tqcovs\tqseq\tsseq" | cat - "$OUT_DOCKER.tmp" > "$OUT_DOCKER.tmpfile2" && mv "$OUT_DOCKER.tmpfile2" "$OUT_DOCKER.tmp"

cp "$OUT_DOCKER" "$OUTPUT"

OUTPUT_RAW="${OUTPUT%.*}_raw.csv"
cp "$OUT_DOCKER".tmp "$OUTPUT_RAW"

rm -r "$TMP_DIR"

echo "[INFO] Finished! Filtered results saved to: $OUTPUT"
