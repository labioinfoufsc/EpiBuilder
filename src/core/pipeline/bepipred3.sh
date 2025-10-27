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
BASE_DIR="/tmp/epibuilder/bepipred"

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

echo "Checking for GPU support..."

# Desativa falha temporariamente só para o teste da GPU
set +e
docker run --rm --runtime=nvidia --gpus all ubuntu nvidia-smi > /dev/null 2>&1
GPU_STATUS=$?
set -e  # Reativa modo estrito para o restante do script

if [ $GPU_STATUS -eq 0 ]; then
    echo "✅ GPU detected, running with GPU support..."
    GPU_OPTS="--runtime=nvidia --gpus all"
else
    echo "⚠️ No GPU available or error occurred, running on CPU..."
    GPU_OPTS=""
fi

EPIBUILDER_VOLUME="${EPIBUILDER_VOLUME:-/tmp/epibuilder}"

# Executa o contêiner normalmente
docker run --rm $GPU_OPTS \
    -v "$EPIBUILDER_VOLUME:/tmp/epibuilder" \
    bioinfoufsc/bepipred3 \
    python3 -u ./bepipred3_custom.py \
    -i "$TMP_DIR/$FILENAME" -o "$TMP_DIR" \
    2>&1 | tee -a "$ORIG_DIR/pipeline.log"

echo "[INFO] Analysis completed inside container."

cp -r "$TMP_DIR"/* "$ORIG_DIR/"

echo "[INFO] Results copied to: $ORIG_DIR"

rm -rf "$TMP_DIR"
