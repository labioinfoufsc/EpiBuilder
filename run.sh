#!/bin/bash

# Defina o nome da pasta do usuário aqui
USER_FOLDER="bruna_new"

# Caminhos base
PIPELINE_DIR="/pipeline"
FASTA_DIR="$PIPELINE_DIR/fasta"
USER_PATH="$PIPELINE_DIR/$USER_FOLDER"

# Comando Nextflow
nextflow run $PIPELINE_DIR/main.nf \
    -with-timeline $USER_PATH/timeline.html \
    --input_file $USER_PATH/ \
    --basename $USER_PATH \
    --search blast \
    --proteomes iedb=$FASTA_DIR/iedb.fasta
