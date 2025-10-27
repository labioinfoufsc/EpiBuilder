#!/bin/bash
set -e

# -------------------------------
# Verifica parâmetros
# -------------------------------
if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $0 /path/to/input.fasta loc"
    echo "loc must be one of: animal, fungi, plant, arch, gram_pos, gram_neg, none"
    exit 1
fi

INPUT_PATH="$1"
LOC="$2"

# Garante que o arquivo existe
if [ ! -f "$INPUT_PATH" ]; then
    echo "Error: File not found: $INPUT_PATH"
    exit 1
fi

# -------------------------------
# Define diretórios
# -------------------------------
if [[ "$LOC" =~ ^(animal|fungi|plant)$ ]]; then
    BASE_DIR="/tmp/epibuilder/wolfpsort"
else
    BASE_DIR="/tmp/epibuilder/psortb"
fi

FILENAME=$(basename "$INPUT_PATH")
ORIG_DIR=$(dirname "$INPUT_PATH")
RANDOM_ID=$(cat /proc/sys/kernel/random/uuid)
TMP_DIR="$BASE_DIR/$RANDOM_ID"

mkdir -p "$TMP_DIR"
cp "$INPUT_PATH" "$TMP_DIR/$FILENAME"

echo "[INFO] Copied $INPUT_PATH to $TMP_DIR/$FILENAME"



  # -------------------------------
  # Execute WolfPsort ou PSORTb
  # -------------------------------
  EPIBUILDER_VOLUME="${EPIBUILDER_VOLUME:-/tmp/epibuilder}"

  if [[ "$LOC" =~ ^(animal|fungi|plant)$ ]]; then
      RAW_FILE="$TMP_DIR/raw_subcell.txt"
      TSV_FILE="$TMP_DIR/localization.tsv"
      echo "[INFO] Running WolfPsort for $LOC..."
      docker run --rm \
          -v "$EPIBUILDER_VOLUME:/tmp/epibuilder" \
          bioinfoufsc/wolfpsort \
          -i "$TMP_DIR/$FILENAME" -s "$LOC" -o "$RAW_FILE" \
          2>&1 | tee -a "$ORIG_DIR/pipeline.log"

      echo "[INFO] Converting WolfPsort output to localization.tsv..."

      # Cabeçalho do TSV
      echo -e "SeqID\tLocalization\tScore" > "$TSV_FILE"

      # Mapeamento antigo -> novo
      declare -A LOC_MAP=(
          ["cyts"]="Cytoskeleton"
          ["cyto"]="Cytosol"
          ["E.R."]="Endoplasmic Reticulum"
          ["extr"]="Extracellular"
          ["golg"]="Golgi apparatus"
          ["mito"]="Mitochondrion"
          ["nucl"]="Nucleus"
          ["plas"]="Plasma membrane"
          ["pero"]="Peroxisome"
          ["vacu"]="Vacuolar membrane"
          ["chlo"]="Chloroplast"
      )

      # Processa linha a linha
      tail -n +2 "$RAW_FILE" | while read -r line; do
          [[ -z "$line" || "$line" == \#* ]] && continue

          prediction=$(echo "$line" | cut -d',' -f1)
          parts=($prediction)

          seq_id=${parts[0]}
          loc_key=${parts[1]}
          score=${parts[2]}

          loc_name=${LOC_MAP[$loc_key]:-Unknown}

          echo -e "$seq_id\t$loc_name\t$score" >> "$TSV_FILE"
      done

  elif [[ "$LOC" =~ ^(arch|gram_pos|gram_neg)$ ]]; then
      declare -A PSORT_FLAGS=( ["arch"]="-a" ["gram_pos"]="-p" ["gram_neg"]="-n" )
      FLAG=${PSORT_FLAGS[$LOC]}

      if [ -z "$FLAG" ]; then
          echo "Error: Invalid 'loc' for PSORTb. Use arch, gram_pos, or gram_neg."
          exit 1
      fi

      echo "[INFO] Running PSORTb for $LOC ($FLAG)..."
      docker run --rm \
          -v "$EPIBUILDER_VOLUME:/tmp/epibuilder" \
           bioinfoufsc/psortb \
          -i "$TMP_DIR/$FILENAME" $FLAG -o terse -r "$TMP_DIR/localization.tsv" \
          2>&1 | tee -a "$ORIG_DIR/pipeline.log"
  else
    # -------------------------------
    # Caso LOC = none → gera TSV vazio
    # -------------------------------
    TSV_FILE="$ORIG_DIR/localization.tsv"
    echo -e "SeqID\tLocalization\tScore" > "$TSV_FILE"
    echo "[INFO] LOC=none → Created empty localization file at: $TSV_FILE"
fi
# -------------------------------
# Copia resultados de volta
# -------------------------------
cp -r "$TMP_DIR"/* "$ORIG_DIR/"
echo "[INFO] Results copied to: $ORIG_DIR"

# -------------------------------
# Limpa temporário
# -------------------------------
rm -rf "$TMP_DIR"
echo "[INFO] Temporary directory removed."
