#!/bin/bash

# Check if a text argument was provided
if [ -z "$1" ]; then
    echo "Usage: $0 'FASTA sequence or text'"
    exit 1
fi

# Store the input text
INPUT_TEXT="$1"

# Create temporary directory and files
TMPDIR=$(mktemp -d)
INPUT_FILE="$TMPDIR/input.fasta"
OUTPUT_FILE="$TMPDIR"

# Write the text into the input file
echo "$INPUT_TEXT" > "$INPUT_FILE"

# Run Bepipred3
python bepipred3_CLI.py -i "$INPUT_FILE" -pred vt_pred -o "$OUTPUT_FILE" >/dev/null 2>&1

# Check for errors
if [ $? -ne 0 ]; then
    echo "Error running bepipred3_CLI.py"
    rm -rf "$TMPDIR"
    exit 1
fi

# Display the output
cat "$OUTPUT_FILE/raw_output.csv"

# Clean up temporary directory
rm -rf "$TMPDIR"