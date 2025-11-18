#!/bin/bash

show_help() {
    echo ""
    echo "Usage: epibuilder [OPTIONS]"
    echo ""
    echo "Required parameters:"
    echo "  --input_file PATH          Path to the input file"
    echo ""
    echo "Optional parameters:"
    echo "  --loc                      Subcellular localization: 'animal', 'plant', 'fungi', 'arch', 'gram_pos', 'gram_neg'"
    echo "                              animal     - Eukaryote - Animal"
    echo "                              plant      - Eukaryote - Plant"
    echo "                              fungi      - Eukaryote - Fungi"
    echo "                              arch       - Archaea"
    echo "                              gram_pos   - Bacteria Gram +"
    echo "                              gram_neg   - Bacteria Gram -"
    echo "  --min-length INT           Minimum length"
    echo "  --max-length INT           Maximum length"
    echo "  --threshold FLOAT          Threshold value"
    echo "  --output NAME              Basename for output files"
    echo "  --search MODE              Search mode: 'none', 'blast', etc. (default: 'none')"
    echo "  --proteomes FILE           Path to proteomes file (format: alias1=path1:alias2=path2)"
    echo "  --cover INT                BLAST minimum coverage cutoff (default: 90)"
    echo "  --identity INT             BLAST minimum identity cutoff (default: 90)"
    echo "  --bepipred_batch INT       Maximum number of proteins submitted for Bepipred processing (default: 100)"
    echo "  --bepipred_gpu             Enable GPU acceleration for Bepipred (default: false)"
    echo "  --help                     Show this help message and exit"
    echo ""
    echo "Available databases:"
    echo "  /db/iedb.fasta"
    echo "      Epitope sequences obtained from https://www.iedb.org/"
    echo ""
    echo "  /db/uniprot.fasta"
    echo "      Protein sequences obtained from https://www.uniprot.org/"
    if [ -f /db/uniprot_release_notes.txt ]; then
        RELEASE_LINE=$(head -n 1 /db/uniprot_release_notes.txt)
        echo "      Release: $RELEASE_LINE"
    fi
    echo ""
    echo "Example:"
    echo "  epibuilder --input_file /fasta/ebola.fasta --min-length 8 --max-length 30 --threshold 0.1512 --output /data/result_ebola \\"
    echo "             --proteomes iedb=/db/iedb.fasta:uniprot=/db/uniprot.fasta --cover 85 --identity 80"
    echo ""
    echo "  # With GPU acceleration:"
    echo "  epibuilder --input_file /fasta/ebola.fasta --bepipred_gpu"
    echo ""
}

# Default values
SEARCH="none"
COVER=90
IDENTITY=90
WORD_SIZE=4
BEPIPRED_BATCH=100
BEPIPRED_GPU="false"

SCRIPT_PATH="$(readlink -f "$0")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
MAIN_NF="$SCRIPT_DIR/main.nf"
JAR_PATH="$SCRIPT_DIR/epibuilder-core.jar"

# Parse arguments
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        --input_file)
            INPUT_FILE="$2"
            shift
            shift
            ;;
        --loc)
            LOC="$2"
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
        --output)
            OUTPUT_DIR="$2"
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
        --cover)
            COVER="$2"
            shift
            shift
            ;;
        --identity)
            IDENTITY="$2"
            shift
            shift
            ;;
        --word-size)
            WORD_SIZE="$2"
            shift
            shift
            ;;
        --bepipred_batch)
            BEPIPRED_BATCH="$2"
            shift
            shift
            ;;
        --bepipred_gpu)
            BEPIPRED_GPU="true"
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

[[ -n "$OUTPUT_DIR" ]] && mkdir -p "$OUTPUT_DIR/reports"

# Build Nextflow command with all parameters
NF_CMD="nextflow run \"$MAIN_NF\" --input_file \"$INPUT_FILE\" --search \"$SEARCH\""

[[ -n "$LOC" ]] && NF_CMD+=" --loc \"$LOC\""
[[ -n "$MIN_LENGTH" ]] && NF_CMD+=" --min-length \"$MIN_LENGTH\""
[[ -n "$MAX_LENGTH" ]] && NF_CMD+=" --max-length \"$MAX_LENGTH\""
[[ -n "$THRESHOLD" ]] && NF_CMD+=" --threshold \"$THRESHOLD\""
[[ -n "$OUTPUT_DIR" ]] && NF_CMD+=" --output_dir \"$OUTPUT_DIR\""
[[ -n "$PROTEOMES" ]] && NF_CMD+=" --proteomes \"$PROTEOMES\""
NF_CMD+=" --cover \"$COVER\""
NF_CMD+=" --identity \"$IDENTITY\""
NF_CMD+=" --word-size \"$WORD_SIZE\""
NF_CMD+=" --jar \"$JAR_PATH\""
NF_CMD+=" --bepipred_batch \"$BEPIPRED_BATCH\""
NF_CMD+=" --bepipred_gpu \"$BEPIPRED_GPU\""

# Add Nextflow reports if BASENAME is defined
[[ -n "$OUTPUT_DIR" ]] && NF_CMD+=" \
    -with-report $OUTPUT_DIR/reports/report.html \
    -with-trace $OUTPUT_DIR/reports/trace.txt \
    -with-timeline $OUTPUT_DIR/reports/timeline.html \
    -with-dag $OUTPUT_DIR/reports/flowchart.png"

# Execute
eval $NF_CMD