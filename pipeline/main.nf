#!/usr/bin/env nextflow

nextflow.enable.dsl = 2

params.input_file = params.input_file ?: null

params.minLength   = params.minLength   ?: null
params.maxLength   = params.maxLength   ?: null
params.threshold   = params.threshold   ?: null
params.basename    = params.basename    ?: null
params.search      = params.search      ?: 'none'
params.proteomes   = params.proteomes   ?: null

workflow {
    def input_file_path = file(params.input_file)
    def jar_path = file('/epibuilder/epibuilder-core.jar')
    // Garante que params.basename está definido
    if (!params.basename) {
        error "You must provide a value for 'params.basename'"
    }

    // Cria o diretório se não existir
    def outputDir = new File(params.basename).absoluteFile
    if (!outputDir.exists()) {
        println "Creating output directory: ${outputDir}"
        outputDir.mkdirs()
    } else {
        println "Output directory already exists: ${outputDir}"
    }
    if (params.input_file.endsWith('.fasta')) {
        def result = validate_fasta(params.input_file)
        println "Any valid proteins? ${result.status}"
        if (result.valid_proteins) println "Valid proteins saved at: ${result.valid_proteins}"
        if (result.invalid_proteins) println "Invalid proteins saved at: ${result.invalid_proteins}"

        if (result.status) {
            run_bepipred(result.valid_proteins)
            def epibuilder = run_epibuilder(run_bepipred.out, jar_path)
            copy_results(epibuilder.out).view()
        }else {
            error "Review the fasta file, ${result.invalid_proteins} invalid proteins "
        }
    } else if (params.input_file.endsWith('.csv')) {
        def epibuilder = run_epibuilder(input_file_path, jar_path)
        copy_results(epibuilder.out).view()
    } else {
        error 'Unsupported file type. Use .fasta or .csv'
    }
}

/**
 * Process: copy_results
 * Description:
 *   Copies the results from the generated output directory to a new directory
 *   named after the `params.basename` parameter. It ensures a clean copy of results
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
    echo "Starting copy"
    echo "Basename: ${params.basename}"
    echo "Current dir: \${PWD}"

    # Define temporary path
    final_path="\${PWD}/${params.basename}/temp"

    # Create temp directory
    mkdir -p "\$final_path"

    # Copy new results
    cp -r \$(realpath ${output_dir})/* "\$final_path/"

    # Remove makeblastdb files
    rm -f \$final_path/*.phr \$final_path/*.pin \$final_path/*.pjs \$final_path/*.pot \$final_path/*.ptf \$final_path/*.pto \$final_path/*.psq \$final_path/*.pdb \$final_path/*.pog \$final_path/*.nin \$final_path/*.nsq \$final_path/*.nhr

    # Rename files by removing 'epibuilder-results-' prefix
    if ls \$final_path/epibuilder-results-* 1> /dev/null 2>&1; then
        for file in \$final_path/epibuilder-results-*; do
            newname=\$(basename "\$file" | sed 's/^epibuilder-results-//')
            echo "Renaming: \$(basename "\$file") -> \$newname"
            mv "\$file" "\$final_path/\$newname"
        done
    else
        echo "No files with prefix 'epibuilder-results-' found."
    fi

    # Rename files by removing 'epibuilder-' prefix
    if ls \$final_path/epibuilder-* 1> /dev/null 2>&1; then
        for file in \$final_path/epibuilder-*; do
            newname=\$(basename "\$file" | sed 's/^epibuilder-//')
            echo "Renaming: \$(basename "\$file") -> \$newname"
            mv "\$file" "\$final_path/\$newname"
        done
    else
        echo "No files with prefix 'epibuilder-' found."
    fi

    # Move all contents from temp to final output directory
    echo "Moving results from temp to ${params.basename}"
    mv \$final_path/* "${params.basename}/"

    # Optionally remove empty temp dir
    rmdir "\$final_path"

    echo "Your results are in \$(realpath ${params.basename})"
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

    input:
    path input_file

    output:
    path 'bepipred_output/raw_output.csv', emit: output

    script:
    """
    python3 /bepipred3_CLI.py -i ${input_file} -pred vt_pred -o bepipred_output/
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

    container = ''

    input:
    path input_file
    path epibuilder_jar

    output:
    path 'epibuilder-results', emit: out

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
    if (params.search && params.search != 'none') {
        args << "--search ${params.search}"
    }
    if (params.proteomes) {
        args << "--proteomes '${params.proteomes}'"
    }

    def cmd = "java -jar ${epibuilder_jar} --input ${input_file} --format csv ${args.join(' ')} --output epibuilder-results"

    """
    echo "Running command: ${cmd}"
    ${cmd} > /pipeline/epibuilder.log
    cp ${input_file} epibuilder-results/
    """
}

/*
 * Function: isValidSequence
 * Description:
 *   Validates if a given sequence contains only valid amino acids.
 *
 * Inputs:
 *   - seq: A string representing the amino acid sequence to be validated.
 *
 * Outputs:
 *   - Boolean: true if the sequence is valid, false otherwise.
 */
def isValidSequence(String seq) {
    def validAminoAcids = 'ACDEFGHIKLMNPQRSTVWY'.toSet()
    return seq.every { it in validAminoAcids }
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
def validate_fasta(String fastaPath) {
    println "Current execution directory: ${workflow.workDir}"

    def valid = []
    def invalid = []

    def currentHeader = null
    def currentSeq = new StringBuilder()

    new File(fastaPath).eachLine { line ->
        if (line.startsWith('>')) {
            if (currentHeader != null) {
                def seq = currentSeq.toString().replaceAll('\\s', '').toUpperCase()
                if (isValidSequence(seq)) {
                    valid << [header: currentHeader, seq: seq]
                } else {
                    invalid << [header: currentHeader, seq: seq]
                }
            }
            currentHeader = line
            currentSeq = new StringBuilder()
        } else {
            currentSeq << line.trim()
        }
    }

    // process last sequence
    if (currentHeader != null) {
        def seq = currentSeq.toString().replaceAll('\\s', '').toUpperCase()
        if (isValidSequence(seq)) {
            valid << [header: currentHeader, seq: seq]
        } else {
            invalid << [header: currentHeader, seq: seq]
        }
    }

    //def timestamp = System.currentTimeMillis()
    //def tmpDir = new File("/pipeline/work/tmp/${timestamp}")
    //tmpDir.mkdirs()

    def outputDir = new File(params.basename).absoluteFile

    def result = [:]

    if (valid) {
        def validFile = new File(outputDir, 'proteins_valid.fasta')
        validFile.withWriter { w ->
            valid.each {
                def cleanHeader = it.header.split(' ')[0]
                w.println(cleanHeader)
                w.println(it.seq)
            }
        }
        result.status = true
        result.valid_proteins = validFile.absolutePath
    }

    if (invalid) {
        def invalidFile = new File(outputDir, 'proteins_invalid.fasta')
        invalidFile.withWriter { w ->
            invalid.each {
                def cleanHeader = it.header.split(' ')[0]
                w.println(cleanHeader)
                w.println(it.seq)
            }
        }
        result.invalid_proteins = invalidFile.absolutePath
    }

    println "Valid sequences: ${valid.size()}, Invalid sequences: ${invalid.size()}"
    println "Files saved in: ${outputDir.absolutePath}"

    return result
}
