#!/usr/bin/env nextflow

nextflow.enable.dsl = 2

params.input_file     = params.input_file     ?: null
params.loc            = params.loc            ?: "none"
params.minLength      = params.minLength      ?: null
params.maxLength      = params.maxLength      ?: null
params.threshold      = params.threshold      ?: null
params.output_dir     = params.output_dir     ?: null
params.identity       = params.identity       ?: 90
params.cover          = params.cover          ?: 90
params.proteomes      = params.proteomes      ?: null
params.batch          = params.batch          ?: null
params.bepipred_gpu   = params.bepipred_gpu   ?: false
params.desc_file      = params.desc_file      ?: null
params.pred           = params.pred           ?: "bepipred"


epibuilder_jar = params.jar ? file(params.jar) : file("${projectDir}/epibuilder-core.jar")

process run_blast {
    tag 'blast'
    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path epitope_fasta
    path epitope_detail

    output:
    path "epitope-detail.tsv", emit: epitope_detail_blast
    path "*.csv", emit: blast_csvs

    script:
    """
    #!/bin/bash
    set -e
    java -cp ${epibuilder_jar} Blastp --input ${epitope_fasta} --proteomes ${params.proteomes} --identity ${params.identity} --cover ${params.cover} --report ${epitope_detail} --output epitope-detail.tsv 2>&1 | tee /dev/stderr
    """
}

process run_localization {
    tag 'run_localization'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path input_file
    val localization
    output:
    path 'localization.tsv', emit: output

    script:
    """
    #!/bin/bash
    set -e
    java -cp ${epibuilder_jar} SubcellularLocalization -i ${input_file} -o localization.tsv -loc ${localization} 2>&1 | tee /dev/stderr
    """
}

process export_excel {
    tag "export_excel"

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path epitope_report
    path protein_summary
    path topology

    output:
    path "epibuilder.xlsx", emit: excel_file

    script:
    """
    #!/bin/bash
    set -e
    java -cp ${epibuilder_jar} ExportExcel --detailed ${epitope_report} --protein ${protein_summary} --topology ${topology} --output epibuilder.xlsx
    """
}

process prepare_csv {
    tag "prepare_csv"

    stageInMode 'copy'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path input_file

    output:
    path 'valid_proteins.fasta', emit: valid_proteins

    script:

    def args = []
    args << "--prediction ${params.pred}"

    """
    #!/bin/bash
    set -e
    java -cp ${epibuilder_jar} FastaFromCSV --input ${input_file} --output valid_proteins.fasta ${args.join(' ')}
    """
}

process join_blast_hits {
    tag 'join_blast'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path blast_csvs
    path epitope_detail

    output:
    path 'epitope-detail.tsv', emit: joined_blast

    script:
    """
    #!/bin/bash
    set -e
    java -cp ${epibuilder_jar} JoinBlastHits --report ${epitope_detail} --identity ${params.identity} --cover ${params.cover} --blast ${blast_csvs.join(' ')} --output epitope-detail.tsv
    """
}

process validate_proteomes_fasta {
    tag 'validate_proteomes_fasta'

    input:
    val proteomes

    script:
    """
    #!/bin/bash
    set -e
    java -cp ${epibuilder_jar} ProteomesValidation --proteomes ${proteomes}
    """
}

/*
 * Process: run_bepipred
 * Description:
 *   Executes the BepiPred3 CLI tool to predict B-cell epitopes from a FASTA input file.
 *
 * Inputs:
 *   - input_file: A FASTA file with protein sequences.
 *
 * Outputs:
 *   - A CSV file with raw BepiPred output located at bepipred_output/raw_output.csv.
 */


process run_bepipred {
    tag 'bepipred'

    publishDir "${params.output_dir}",
        mode: 'copy',
        overwrite: true

    input:
    path input_file

    output:
    path 'raw_output.csv', emit: output

    script:
    def gpuFlag = params.bepipred_gpu.toString().toBoolean() ? "--use-gpu" : ""

    // Only pass batch size when explicitly provided
    def batchFlag = params.batch != null ?
        "-s ${params.batch}" : ""


    """
    #!/bin/bash
    set -euo pipefail

    mkdir -p "${params.output_dir}/reports"
    LIVE_LOG="${params.output_dir}/reports/bepipred-live.log"

    {
        echo "[INFO] Starting BepiPred3..."
        echo "[INFO] Batch size: ${params.batch ?: 'default'}"
        echo "[INFO] GPU enabled: ${params.bepipred_gpu}"

        java -cp ${epibuilder_jar} Bepipred3 \
            -i ${input_file} \
            -o raw_output.csv \
            ${batchFlag} \
            ${gpuFlag}

    } 2>&1 | tee "\$LIVE_LOG" /proc/1/fd/1
    """
}

process run_epidope {
    tag 'epidope'

    //publishDir resultsDir, mode: 'copy', overwrite: true, pattern: 'epidope.csv'
    publishDir "${params.output_dir}",
            mode: 'copy',
            overwrite: true

    input:
    path input_file

    output:
    path 'epidope.csv', emit: output

    script:

    def batchFlag = params.batch != null ?
            "-s ${params.batch}" : ""

    """
    #!/usr/bin/env bash
    set -euo pipefail

    # Only pass batch size when explicitly provided
    echo "[INFO] Running Epidope (batch=${params.batch ?: 'default'})"
    # Running Epidope

    java -cp "${epibuilder_jar}" Epidope -i "${input_file}" -o epidope.csv ${batchFlag}

    """
}


/*
 * Process: run_epibuilder
 * Description:
 *   Runs the EpiBuilder Java application to process epitope prediction results.
 *   Accepts additional parameters for fine-tuning such as minimum/maximum epitope lengths,
 *   prediction threshold, and optional search settings using UniProt proteomes.
 *
 * Inputs:
 *   - input_file: A CSV file (either from BepiPred or provided directly).
 *   - epibuilder_jar: The EpiBuilder JAR file (epibuilder-core.jar).
 *
 * Outputs:
 *   - A directory named epibuilder-results containing the processed output and the original input file.
 */
process run_epibuilder {
    tag 'epibuilder'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    container = ''

    input:
    path input_file
    path desc_file
    path localization_file

    output:
    path 'epitopes.fasta', emit: epitopes
    path 'epitope-detail.tsv', emit: epitope_detail
    path 'topology.tsv', emit: topology
    path 'protein-summary.tsv', emit: protein_summary
    path 'scores.tsv', emit: scores

    script:
    def args = []

    args << "--prediction ${params.pred}"

    if (params.minLength) {
        args << "--min-length ${params.minLength}"
    }
    if (params.maxLength) {
        args << "--max-length ${params.maxLength}"
    }
    if (params.threshold) {
        args << "--threshold ${params.threshold}"
    }

    def descArg = desc_file ? "--description_file ${desc_file}" : ""
    def localizationArg = localization_file ? "--localization_file ${localization_file}" : ""

    """
    java -jar ${epibuilder_jar} --input ${input_file} ${descArg} ${localizationArg} ${args.join(' ')}
    """
}

/**
 * Function: validate_fasta
 * Description:
 *   Validates a FASTA file by checking if each sequence contains only valid amino acids.
 *   Writes valid sequences to proteins_valid.fasta and invalid sequences to proteins_invalid.fasta.
 *
 * Inputs:
 *   - fastaPath: The path to the FASTA file to be validated.
 *
 * Outputs:
 *   - Two files: proteins_valid.fasta and proteins_invalid.fasta containing valid and invalid sequences respectively.
 */
 process validate_fasta {
     tag 'validate_fasta'

     publishDir "${params.output_dir}", mode: 'copy', overwrite: true

     input:
         path input_fasta

     output:
         path('proteins_valid.fasta'), emit: valid_proteins
         path('proteins_invalid.fasta'), emit: invalid_proteins
         path('description.tsv'), emit: description

     script:
         """
         echo "[INFO] Running FastaValidation..."
         echo "[INFO] Input: ${input_fasta}"
         echo "[INFO] Jar: ${epibuilder_jar}"
         echo "[INFO] Working directory: \$(pwd)"

         java -cp ${epibuilder_jar} FastaValidation --input ${input_fasta} --valid proteins_valid.fasta --invalid proteins_invalid.fasta --description description.tsv
         """
 }

workflow {
    def input_file_path = file(params.input_file)
    def files

    // Validações de parâmetros obrigatórios
    if (!params.input_file) {
        error "You must provide a value for 'params.input_file' using --input_file"
    }

    if (!params.output_dir) {
        error "You must provide a value for 'params.output_dir' using --output_dir"
    }
    if (!params.output_dir) {
        error "You must provide a value for 'params.output_dir'"
    }
    // Cria o diretório se não existir
    def outputDir = new File(params.output_dir).absoluteFile

    if (!outputDir.exists()) {
        println "Creating output directory: ${outputDir}"
        outputDir.mkdirs()
    } else {
        println "Output directory already exists: ${outputDir}"
    }
    //Validate proteomes before all process to avoid unnecessary processing
    if (params.proteomes) {
        validate_proteomes_fasta(params.proteomes)
    }
     // Declara channels e variáveis
    def valid_proteins_ch
    def description_ch
    def epibuilder_input_ch  // Pode ser bepipred output ou CSV direto

    // Processa o input dependendo do tipo
    if (params.input_file.endsWith('.fasta')) {
            // Validate FASTA
            def fasta_files = validate_fasta(input_file_path)

            valid_proteins_ch = fasta_files.valid_proteins
            description_ch = fasta_files.description

            // Select prediction method
            def prediction_output_ch

            if (params.pred == 'bepipred') {
                prediction_output_ch = run_bepipred(
                    fasta_files.valid_proteins
                ).output

            } else if (params.pred == 'epidope') {
                prediction_output_ch = run_epidope(
                    fasta_files.valid_proteins
                ).output

            } else {
                error "Unknown prediction method: ${params.pred}"
            }

            // Prediction output for EpiBuilder
            epibuilder_input_ch = prediction_output_ch

            // Wait for all prediction tasks to complete
            def prediction_done_ch = prediction_output_ch
                .collect()
                .map { true }

            // Localization starts only after prediction completes
            valid_proteins_ch = valid_proteins_ch
                .combine(prediction_done_ch)
                .map { proteins, done -> proteins }

    } else if (params.input_file.endsWith('.csv')) {
        // Para CSV: prepara -> valida -> epibuilder (sem bepipred)
        def csv_files = prepare_csv(input_file_path)
        def csv_validated = validate_fasta(csv_files.valid_proteins)
        valid_proteins_ch = csv_validated.valid_proteins
        description_ch = params.desc_file ?
            Channel.value(file(params.desc_file)) :
            csv_validated.description
        epibuilder_input_ch = input_file_path  // CSV vai direto para epibuilder

    } else {
        error 'Unsupported file type. Use .fasta or .csv'
    }

    // Processos comuns para ambos os tipos
    def localization_out = run_localization(valid_proteins_ch, params.loc)

    // Executa epibuilder com os inputs apropriados
    epibuilder = run_epibuilder(
        epibuilder_input_ch,      // FASTA: output do bepipred | CSV: arquivo original
        description_ch,
        localization_out.output
    )

    // Executing blast
    if (params.proteomes) {
        def blast = run_blast(epibuilder.epitopes, epibuilder.epitope_detail)
        export_excel(blast.epitope_detail_blast, epibuilder.protein_summary, epibuilder.topology)
    } else {
        export_excel(epibuilder.epitope_detail, epibuilder.protein_summary, epibuilder.topology)
    }

}
