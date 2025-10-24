#!/bin/bash
set -e

# ============ Checagem de parâmetros ============
if [ $# -ne 3 ]; then
    echo "Usage: $0 /path/to/input.fasta /path/to/database.fasta /path/to/output.txt"
    exit 1
fi

INPUT_PATH="$1"
DB_PATH="$2"
OUTPUT="$3"

# Verifica existência dos arquivos de entrada
if [ ! -f "$INPUT_PATH" ]; then
    echo "[ERROR] Input file not found: $INPUT_PATH"
    exit 1
fi
if [ ! -f "$DB_PATH" ]; then
    echo "[ERROR] Database file not found: $DB_PATH"
    exit 1
fi

# Diretório base e temporário
BASE_DIR="/tmp/epibuilder/diamond"
mkdir -p "$BASE_DIR"
RANDOM_ID=$(cat /proc/sys/kernel/random/uuid)
TMP_DIR="$BASE_DIR/$RANDOM_ID"
mkdir -p "$TMP_DIR"

# Nomes base
FILENAME=$(basename "$INPUT_PATH")
DB_BASENAME=$(basename "$DB_PATH")
ORIG_DIR=$(dirname "$INPUT_PATH")

# ============ Lógica do banco BLAST ============
if [ -f "${DB_PATH}.dmnd" ]; then
    echo "[INFO] Existing DIAMOND database found: ${DB_PATH}.dmnd"
    echo "[INFO] Linking all files with prefix ${DB_PATH}* ..."

    cp "${DB_PATH}.dmnd" "$TMP_DIR/$(basename "${DB_PATH}.dmnd")"
else
    echo "[INFO] No BLAST database found. Linking FASTA and generating..."
    cp "$DB_PATH" "$TMP_DIR/$DB_BASENAME"

    docker run --rm \
        -v epibuilder-data:/tmp/epibuilder \
        staphb/diamond:2.1.13 \
        diamond makedb --in "$TMP_DIR/$DB_BASENAME" -d "$TMP_DIR/$DB_BASENAME.dmnd"
fi


# Copia o arquivo de entrada
cp "$INPUT_PATH" "$TMP_DIR/$FILENAME"
echo "[INFO] Copied input file to $TMP_DIR/$FILENAME"

# ============ Executa BLASTP ============
echo "[INFO] Running BLASTP..."
docker run --rm \
    -v epibuilder-data:/tmp/epibuilder \
    staphb/diamond:2.1.13 \
    diamond blastp -q "$TMP_DIR/$FILENAME" -d "$TMP_DIR/$DB_BASENAME.dmnd" \
           -o "$TMP_DIR/$(basename "$OUTPUT")" \
           -f 6 qseqid sseqid pident qcovhsp qseq sseq \
    2>&1 | tee -a "$ORIG_DIR/pipeline.log"

echo "[INFO] Analysis completed."

echo "[INFO] Adding header to output..."
{
  printf "qacc\tsacc\tpident\tqcovs\tqseq\tsseq\n"
  cat "$TMP_DIR/$(basename "$OUTPUT")"
} > "$OUTPUT"
echo "[INFO] Header added to: $OUTPUT"

# Limpeza
#rm -rf "$TMP_DIR"
echo "[INFO] Temporary files removed."
