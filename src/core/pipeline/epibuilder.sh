#!/usr/bin/env bash
set -euo pipefail

show_help() {
    cat <<'HELP'
Usage: epibuilder.sh --input_file PATH --output DIR [OPTIONS]

Required parameters:
  --input_file PATH          Path to input FASTA or CSV file
  --output DIR               Directory for results and Nextflow reports

Optional parameters:
  --pred MODE                Prediction method: bepipred (default) or epidope
  --desc_file PATH           Protein descriptions TSV (ID, description)
  --loc TYPE                 Localization: none, animal, plant, fungi, arch,
                             gram_pos or gram_neg
  --min-length INT           Minimum epitope length
  --max-length INT           Maximum epitope length
  --threshold FLOAT          Score threshold
  --search MODE              Search mode (default: none)
  --proteomes STRING         Proteomes as alias=path:alias2=path2
  --cover INT                BLAST minimum coverage (default: 90)
  --identity INT             BLAST minimum identity (default: 90)
  --word-size INT            BLAST word size (default: 4)
  --batch INT       Optional batch size; omitted by default
  --bepipred_gpu             Enable GPU acceleration for BepiPred
  --task_id INT              Internal task identifier
  --debug                    Show Nextflow process output, disable ANSI logging
  --help                     Show this help

Examples:
  epibuilder.sh --input_file fasta/input.fasta --output results --pred bepipred
  epibuilder.sh --input_file fasta/input.fasta --output results --pred epidope --batch 500
HELP
}

# Default values
INPUT_FILE=""
OUTPUT_DIR=""
DESC_FILE=""
LOC=""
MIN_LENGTH=""
MAX_LENGTH=""
THRESHOLD=""
PROTEOMES=""
SEARCH="none"
COVER="90"
IDENTITY="90"
WORD_SIZE="4"
BATCH=""
BEPIPRED_GPU="false"
PRED="bepipred"
DEBUG="false"
TASK_ID=""

SCRIPT_PATH="$(readlink -f "$0")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
MAIN_NF="$SCRIPT_DIR/main.nf"
JAR_PATH="$SCRIPT_DIR/epibuilder-core.jar"

# Parse arguments
while (( $# > 0 )); do
    case "$1" in
        --help|-h)
            show_help
            exit 0
            ;;

        --bepipred_gpu)
            BEPIPRED_GPU="true"
            shift
            ;;

        --debug)
            DEBUG="true"
            shift
            ;;

        --input_file|--output|--desc_file|--loc|--min-length|--max-length|\
        --threshold|--search|--proteomes|--cover|--identity|--word-size|\
        --batch|--pred|--task_id)
            key="$1"

            if (( $# < 2 )) || [[ -z "$2" || "$2" == --* ]]; then
                echo "ERROR: $key requires a value" >&2
                exit 2
            fi

            value="$2"

            case "$key" in
                --input_file) INPUT_FILE="$value" ;;
                --output) OUTPUT_DIR="$value" ;;
                --desc_file) DESC_FILE="$value" ;;
                --loc) LOC="$value" ;;
                --min-length) MIN_LENGTH="$value" ;;
                --max-length) MAX_LENGTH="$value" ;;
                --threshold) THRESHOLD="$value" ;;
                --search) SEARCH="$value" ;;
                --proteomes) PROTEOMES="$value" ;;
                --cover) COVER="$value" ;;
                --identity) IDENTITY="$value" ;;
                --word-size) WORD_SIZE="$value" ;;
                --batch) BATCH="$value" ;;
                --pred) PRED="${value,,}" ;;
                --task_id) TASK_ID="$value" ;;
            esac

            shift 2
            ;;

        *)
            echo "ERROR: unknown parameter '$1'" >&2
            show_help >&2
            exit 2
            ;;
    esac
done

# Validate required parameters
[[ -n "$INPUT_FILE" ]] || {
    echo 'ERROR: --input_file is required' >&2
    exit 2
}

[[ -n "$OUTPUT_DIR" ]] || {
    echo 'ERROR: --output is required' >&2
    exit 2
}

# Validate prediction method
case "$PRED" in
    bepipred|epidope)
        ;;
    *)
        echo 'ERROR: --pred must be bepipred or epidope' >&2
        exit 2
        ;;
esac

# Build Nextflow command
cmd=(nextflow run "$MAIN_NF"
    --input_file "$INPUT_FILE"
    --search "$SEARCH"
    --output_dir "$OUTPUT_DIR"
    --cover "$COVER"
    --identity "$IDENTITY"
    --word-size "$WORD_SIZE"
    --jar "$JAR_PATH"
    --bepipred_gpu "$BEPIPRED_GPU"
    --pred "$PRED"
)

# Optional parameters
[[ -n "$DESC_FILE" ]] && cmd+=(--desc_file "$DESC_FILE")
[[ -n "$LOC" ]] && cmd+=(--loc "$LOC")
[[ -n "$MIN_LENGTH" ]] && cmd+=(--min-length "$MIN_LENGTH")
[[ -n "$MAX_LENGTH" ]] && cmd+=(--max-length "$MAX_LENGTH")
[[ -n "$THRESHOLD" ]] && cmd+=(--threshold "$THRESHOLD")
[[ -n "$PROTEOMES" ]] && cmd+=(--proteomes "$PROTEOMES")
[[ -n "$BATCH" ]] && cmd+=(--batch "$BATCH")

# Enable Nextflow debug
if [[ "$DEBUG" == true ]]; then
    cmd+=(-process.echo -ansi-log false)
fi

# Nextflow reports
mkdir -p "$OUTPUT_DIR/reports"

cmd+=(
    -with-report "$OUTPUT_DIR/reports/report.html"
    -with-trace "$OUTPUT_DIR/reports/trace.txt"
    -with-timeline "$OUTPUT_DIR/reports/timeline.html"
    -with-dag "$OUTPUT_DIR/reports/flowchart.png"
)

printf 'Prediction method: %s\n' "$PRED"
printf 'Nextflow command: '
printf '%q ' "${cmd[@]}"
printf '\n'

# Execute pipeline
if "${cmd[@]}"; then
    echo 'Pipeline finished successfully'

    if [[ -n "$TASK_ID" ]]; then
        curl -f -X POST \
            "http://epibuilder-web:8080/epitopes/tasks/${TASK_ID}/complete"
    fi
else
    status=$?
    echo "Pipeline failed with exit code $status" >&2
    exit "$status"
fi
