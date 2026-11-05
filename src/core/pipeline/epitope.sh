#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
    cat <<'HELP'
Usage:
  epitope.sh --input FILE.fasta --batch SIZE
  epitope.sh --help

Required parameters:
  --input FILE.fasta   FASTA input file
  --batch SIZE         Positive integer, used by Epidope and BepiPred3

Workflow:
  validate_fasta -> run_epidope -> run_bepipred

Outputs (relative to the directory where you launch the script):
  ./epitope_results/epidope.csv
  ./epitope_results/bepipred.csv

The following files must be in the same directory as this script:
  epitope.nf
  epibuilder-core.jar
HELP
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 2
}

INPUT=''
BATCH=''

while (( $# > 0 )); do
    case "$1" in
        --input)
            (( $# >= 2 )) || fail '--input requires a value'
            INPUT="$2"
            shift 2
            ;;
        --batch)
            (( $# >= 2 )) || fail '--batch requires a value'
            BATCH="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            fail "Unknown argument: $1 (use --help)"
            ;;
    esac
done

[[ -n "$INPUT" ]] || fail '--input is required'
[[ -f "$INPUT" ]] || fail "FASTA file not found: $INPUT"
[[ "$BATCH" =~ ^[1-9][0-9]*$ ]] || fail '--batch must be a positive integer'

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PIPELINE="${SCRIPT_DIR}/epitope.nf"
JAR="${SCRIPT_DIR}/epibuilder-core.jar"

[[ -f "$PIPELINE" ]] || fail "Nextflow pipeline not found: $PIPELINE"
[[ -f "$JAR" ]] || fail "Java JAR not found: $JAR"
command -v nextflow >/dev/null 2>&1 || fail 'nextflow command not found'

INPUT="$(realpath -- "$INPUT")"

echo "Input:   $INPUT"
echo "Batch:   $BATCH"
echo "Output:  ${PWD}/epitope_results/{epidope,bepipred}.csv"

exec nextflow run "$PIPELINE" \
    --input "$INPUT" \
    --batch "$BATCH"
