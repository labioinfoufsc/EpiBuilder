#!/bin/bash

set -e

DEV_DB_DIR="/tmp/epibuilder/db"
CORE_DB_DIR="../src/core/db"

echo "Creating development database directory..."
mkdir -p "$DEV_DB_DIR"

echo "Copying IEDB database..."
cp "$CORE_DB_DIR/iedb.fasta" "$DEV_DB_DIR/"

if ls "$DEV_DB_DIR"/uniprot_sprot_*.fasta 1> /dev/null 2>&1; then
    echo "UniProt database already exists. Skipping download."
else
    echo "Downloading UniProt database..."

    curl -L \
        -o "$DEV_DB_DIR/uniprot_sprot.fasta.gz" \
        https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz

    gunzip -f "$DEV_DB_DIR/uniprot_sprot.fasta.gz"

    DATE=$(date +%Y_%m_%d)

    mv "$DEV_DB_DIR/uniprot_sprot.fasta" \
       "$DEV_DB_DIR/uniprot_sprot_${DATE}.fasta"

    echo "UniProt database downloaded successfully."
fi

echo
echo "Development databases ready in:"
echo "$DEV_DB_DIR"
