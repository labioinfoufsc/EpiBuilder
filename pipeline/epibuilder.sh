#!/bin/bash

show_help() {
    echo ""
    echo "Usage: ./epibuilder.sh [OPTIONS]"
    echo ""
    echo "Required parameters:"
    echo "  --input_file PATH      Path to the input file"
    echo ""
    echo "Optional parameters:"
    echo "  --minLength INT        Minimum length"
    echo "  --maxLength INT        Maximum length"
    echo "  --threshold FLOAT      Threshold value"
    echo "  --basename NAME        Basename for output files"
    echo "  --search MODE          Search mode: 'none', 'blast', etc. (default: 'none')"
    echo "  --proteomes FILE       Path to proteomes file"
    echo "  --help                 Show this help message and exit"
    echo ""
    echo "Example:"
    echo "  ./epibuilder.sh --input_file data.fasta --minLength 8 --maxLength 30 --threshold 0.8 --basename result --search blast --proteomes proteomes.fa"
    echo ""
}

# Default values
SEARCH="none"

# Parse arguments
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
    --input_file)
        INPUT_FILE="$2"
        shift
        shift
        ;;
    --min-length)
        MIN_LENGTH="$2"
        shift
        shift
        ;;
    --max-length)
        MAX_LENGTH="$2"
        shift
        shift
        ;;
    --threshold)
        THRESHOLD="$2"
        shift
        shift
        ;;
    --basename)
        BASENAME="$2"
        shift
        shift
        ;;
    --search)
        SEARCH="$2"
        shift
        shift
        ;;
    --proteomes)
        PROTEOMES="$2"
        shift
        shift
        ;;
    --help)
        show_help
        exit 0
        ;;
    *)
        echo "Error: Unknown parameter '$1'"
        show_help
        exit 1
        ;;
    esac
done

# Check required parameters
if [[ -z "$INPUT_FILE" ]]; then
    echo "Error: --input_file is required"
    show_help
    exit 1
fi

# Build Nextflow command
NF_CMD="nextflow run /pipeline/main.nf --input_file \"$INPUT_FILE\" --search \"$SEARCH\""

[[ -n "$MIN_LENGTH" ]] && NF_CMD+=" --min-length \"$MIN_LENGTH\""
[[ -n "$MAX_LENGTH" ]] && NF_CMD+=" --max-length \"$MAX_LENGTH\""
[[ -n "$THRESHOLD" ]] && NF_CMD+=" --threshold \"$THRESHOLD\""
[[ -n "$BASENAME" ]] && NF_CMD+=" --basename \"$BASENAME\""
[[ -n "$PROTEOMES" ]] && NF_CMD+=" --proteomes \"$PROTEOMES\""

# Execute
eval $NF_CMD
