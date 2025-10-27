#!/usr/bin/env nextflow

nextflow.enable.dsl = 2

params.input_file  = params.input_file  ?: null
params.loc         = params.loc         ?: null
params.minLength   = params.minLength   ?: null
params.maxLength   = params.maxLength   ?: null
params.threshold   = params.threshold   ?: null
params.output_dir  = params.output  ?: null
params.identity    = params.identity    ?: 90
params.cover       = params.cover       ?: 90
params.proteomes   = params.proteomes   ?: null

epibuilder_jar = params.jar ? file(params.jar) : file("${projectDir}/epibuilder-core.jar")


process run_blast {
    tag 'blast'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path epitope_fasta

    script:
    """
    #!/bin/bash
    set -e
    input_path="\$(realpath ${epitope_fasta})"

    # Diretório de saída dentro do workDir
    out_dir="\$(realpath ${params.output_dir})"
    mkdir -p "\$out_dir"

    echo "[INFO] Epitope FASTA: ${epitope_fasta}"
    echo "[INFO] Output directory: \$out_dir"

    proteomes="${params.proteomes ?: ''}"

    if [ -n "\$proteomes" ]; then
        IFS=':' read -ra pairs <<< "\$proteomes"
        for pair in "\${pairs[@]}"; do
            alias="\$(echo \$pair | cut -d'=' -f1)"
            db="\$(echo \$pair | cut -d'=' -f2)"
            out_file="\$out_dir/\${alias}_blast.csv"

            echo "[INFO] Running DIAMOND for alias '\$alias' and DB '\$db'"
            "${projectDir}/blastp_custom.sh" "\$input_path" "\$db" "blastp-short" "4" "\$params.id" "\$params.cov" "\$out_file"
        done
    fi
    """
}

process run_blastp {
    tag 'blastp'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path epitope_fasta   // vem do run_epibuilder

    output:
    path '*.csv', emit: blast_results  // todos os CSVs gerados

    script:
    def proteomes = params.proteomes ?: ''

    // Monta os comandos de forma groovy
    def cmds = []
    if (proteomes) {
        proteomes.split(':').each { pair ->
            def (alias, db) = pair.split('=')
            def outFile = "${alias}_blast.csv"
            cmds << """
                echo "[INFO] Running BLAST for alias '${alias}' and DB '${db}'"
                "${projectDir}/blastp_custom.sh" "${epitope_fasta}" "${db}" "blastp-short" "4" "${params.identity}" "${params.cover}" "${outFile}"
            """.stripIndent()
        }
    }
    """
    ${cmds.join('\n')}
    """
}


process run_localization {
    tag 'localization'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path input_file

    output:
    path 'localization.tsv', emit: output

    script:
    """
    # Absolute path of input_file
    INPUT_PATH="\$(realpath ${input_file})"

    # Run localization
    ${projectDir}/localization.sh ${input_file} ${params.loc}
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
    path "epibuilder-blast.xlsx", emit: excel_file

    script:
    """
    echo "[INFO] Exporting reports from "
    """
    def cmd = "java -cp ${epibuilder_jar} ExportExcel --detailed ${epitope_report} --protein ${protein_summary} --topology ${topology} --output epibuilder-blast.xlsx"
    """
    echo "Exporting excel..."
    echo "Running command: ${cmd}"
    ${cmd} > epibuilder.log
    """
}

process prepare_csv {
    tag "prepare_csv"

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path input_file

    output:
    path 'valid_proteins.fasta', emit: valid_proteins

    script:
    """
    echo "[INFO] Extracting FASTA from ${input_file} using Java..."

    # Executa o programa Java

    echo "[INFO] FASTA file created: valid_proteins.fasta"
    """
    def cmd = "java -cp ${epibuilder_jar} FastaFromBepiPredCSV --input ${input_file} --output valid_proteins.fasta"
    """
    echo "Extracting fasta from csv descriptions..."
    echo "Running command: ${cmd}"
    ${cmd} > epibuilder.log
    """
}

process join_blast_hits {
    tag 'join_blast'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path blast_csvs                // múltiplos arquivos do run_blastp
    path epitope_detail

    output:
    path 'epitope-detail-blast.tsv', emit: joined_blast

    script:
    """
    java -cp ${epibuilder_jar} JoinBlastHits \\
         --report ${epitope_detail} \\
         --identity ${params.identity} \\
         --cover ${params.cover} \\
         --blast ${blast_csvs.join(' ')} \\
         --output epitope-detail-blast.tsv
    """
}

/**
 * Process: copy_results
 * Description:
 *   Copies the results from the generated output directory to a new directory
 *   named after the `params.output_dir` parameter. It ensures a clean copy of results
 *   into a clearly labeled and user-defined location.
 *
 * Inputs:
 *   - output_dir: The directory containing the results to be copied.
 *
 * Outputs:
 *   - stdout: A message confirming the location of the copied results.
 */
process copy_results {
    input:
    path output_dir

    output:
    stdout

    script:
    """
    #echo "Starting copy"
    #echo "Output dir: ${params.output_dir}"
    #echo "Current dir: \${PWD}"

    # Define temporary path
    final_path="\${PWD}/${params.output_dir}/temp"

    # Create temp directory
    mkdir -p "\$final_path"

    # Copy new results
    cp -r \$(realpath ${output_dir})/* "\$final_path/"

    # Rename files by removing 'epibuilder-results-' prefix
    if ls \$final_path/epibuilder-results-* 1> /dev/null 2>&1; then
        for file in \$final_path/epibuilder-results-*; do
            newname=\$(output_dir "\$file" | sed 's/^epibuilder-results-//')
            mv "\$file" "\$final_path/\$newname"
        done
    else
        echo "No files with prefix 'epibuilder-results-' found."
    fi

    # Rename files by removing 'epibuilder-' prefix
    if ls \$final_path/epibuilder-* 1> /dev/null 2>&1; then
        for file in \$final_path/epibuilder-*; do
            newname=\$(output_dir "\$file" | sed 's/^epibuilder-//')
            mv "\$file" "\$final_path/\$newname"
        done
    else
        echo "No files with prefix 'epibuilder-' found."
    fi

    # Move all contents from temp to final output directory
    mv \$final_path/* "${params.output_dir}/"

    # Optionally remove empty temp dir
    rmdir "\$final_path"

    echo "Your results are in \$(realpath ${params.output_dir})"
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

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path input_file

    output:
    path 'raw_output.csv', emit: output

    script:
    """
    # Call bepipred3 script
    ${projectDir}/bepipred3.sh ${input_file}
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

    script:
    def args = []
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

    def cmd = "java -jar ${epibuilder_jar} --input ${input_file} --format csv ${descArg} ${localizationArg} ${args.join(' ')} --output results"

    """
    echo "Running command: ${cmd}"
    ${cmd} > epibuilder.log
    cp -r results/* .
    """
}

process run_epibuilder_csv {
    tag 'epibuilder'

    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    container = ''

    input:
    path input_file
    path localization_file

    output:
    path 'epitopes.fasta', emit: epitopes
    path 'epitope-detail.tsv', emit: epitope_detail
    path 'topology.tsv', emit: topology
    path 'protein-summary.tsv', emit: protein_summary

    script:
    def args = []
    if (params.minLength) {
        args << "--min-length ${params.minLength}"
    }
    if (params.maxLength) {
        args << "--max-length ${params.maxLength}"
    }
    if (params.threshold) {
        args << "--threshold ${params.threshold}"
    }

    def localizationArg = localization_file ? "--localization_file ${localization_file}" : ""

    def cmd = "java -jar ${epibuilder_jar} --input ${input_file} --format csv ${localizationArg} ${args.join(' ')} --output results"

    """
    echo "Running command: ${cmd}"
    ${cmd} > epibuilder.log
    cp -r results/* .
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

     // publica automaticamente no output_dir após terminar
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

         java -cp ${epibuilder_jar} FastaValidation \
             --input ${input_fasta} \
             --valid proteins_valid.fasta \
             --invalid proteins_invalid.fasta \
             --description description.tsv
         """
 }

process rename_report_file {
    tag 'rename_final'

    // publica no mesmo diretório final
    publishDir "${params.output_dir}", mode: 'copy', overwrite: true

    input:
    path final_blast_file  // exemplo: epitope-detail-blast.tsv

    output:
    path 'epitope-detail.tsv', emit: renamed_file

    script:
    """

    if [ -f "${final_blast_file}" ]; then
        cp "${final_blast_file}" epitope-detail.tsv
    else
        echo "[WARN] File not found: ${final_blast_file}"
        touch epitope-detail.tsv
    fi
    """
}


workflow {
    def input_file_path = file(params.input_file)

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

    if (params.input_file.endsWith('.fasta')) {

        def files = validate_fasta(input_file_path)
        if (files.valid_proteins) {
            run_bepipred(files.valid_proteins)
            run_localization(files.valid_proteins)
            def epibuilder = run_epibuilder(run_bepipred.out, files.description, run_localization.output)

            if (params.proteomes) {
                def blast = run_blastp(epibuilder.epitopes)
                def report_blast = join_blast_hits(blast.blast_results.collect(), epibuilder.epitope_detail)
                def rename_report_file = rename_report_file(report_blast)
                export_excel(rename_report_file.renamed_file, epibuilder.protein_summary, epibuilder.topology)
            }else{
                export_excel(epibuilder.epitope_detail, epibuilder.protein_summary, epibuilder.topology)
            }
            //copy_results(epibuilder.out).view()*/
        }else {
            error "Review the fasta file, ${files.invalid_proteins} invalid proteins "
        }
    } else if (params.input_file.endsWith('.csv')) {

        def files = prepare_csv(input_file_path)
        if (files.valid_proteins) {
            run_localization(files.valid_proteins)
            def epibuilder = run_epibuilder_csv(input_file_path, run_localization.output)

            if (params.proteomes) {
                def blast = run_blastp(epibuilder.epitopes)
                def report_blast = join_blast_hits(blast.blast_results.collect(), epibuilder.epitope_detail)
                def rename_report_file = rename_report_file(report_blast)
                export_excel(rename_report_file.renamed_file, epibuilder.protein_summary, epibuilder.topology)
            }else{
                export_excel(epibuilder.epitope_detail, epibuilder.protein_summary, epibuilder.topology)
            }
            //copy_results(epibuilder.out).view()*/
        }else {
            error "Review the fasta file, ${files.invalid_proteins} invalid proteins "
        }
        //copy_results(epibuilder.out).view()
    } else {
        error 'Unsupported file type. Use .fasta or .csv'
    }
}