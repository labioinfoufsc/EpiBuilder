#!/bin/bash
set -e

# Verifica se o par  metro foi informado
if [ -z "$1" ]; then
    echo "Usage: $0 /path/to/input.fasta"
    exit 1
fi

INPUT_PATH="$1"

# Garante que o arquivo existe
if [ ! -f "$INPUT_PATH" ]; then
    echo "Error: File not found: $INPUT_PATH"
    exit 1
fi

# Diret  rio base dentro do volume
BASE_DIR="/data/bepipred"

# Nome do arquivo e diret  rio original
FILENAME=$(basename "$INPUT_PATH")
ORIG_DIR=$(dirname "$INPUT_PATH")

# Cria um ID aleat  rio para isolar as execu    es
RANDOM_ID=$(cat /proc/sys/kernel/random/uuid)

# Caminho tempor  rio dentro do volume
TMP_DIR="$BASE_DIR/$RANDOM_ID"

# Cria o diret  rio tempor  rio local e copia o arquivo para l
mkdir -p "$TMP_DIR"
cp "$INPUT_PATH" "$TMP_DIR/$FILENAME"

echo "[INFO] Copied $INPUT_PATH to $TMP_DIR/$FILENAME"

# Executa o container com o volume montado
docker run --rm \
    -v epibuilder-data:/data \
    bioinfoufsc/bepipred3 \
    python3 ./bepipred3_custom.py \
    -i "$TMP_DIR/$FILENAME" \
    -o "$TMP_DIR"

echo "[INFO] Analysis completed inside container."

cp -r "$TMP_DIR"/* "$ORIG_DIR/"

echo "[INFO] Results copied to: $ORIG_DIR"

rm -rf "$TMP_DIR"
