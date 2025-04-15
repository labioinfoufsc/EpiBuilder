#!/usr/bin/env nextflow

nextflow.enable.dsl=2

params.input_file = params.input_file ?: null

params.minLength   = params.minLength   ?: null
params.maxLength   = params.maxLength   ?: null
params.threshold   = params.threshold   ?: null
params.basename    = params.basename    ?: null
params.search      = params.search      ?: 'none'
params.proteomes   = params.proteomes   ?: null

workflow {
    def input_file_path = file(params.input_file)
    def jar_path = file('epibuilder/epibuilder-core.jar')

    if (params.input_file.endsWith('.fasta')) {
        validate_fasta(params.input_file)
        run_bepipred(input_file_path)
        def epibuilder = run_epibuilder(run_bepipred.out, jar_path)
        copy_results(epibuilder.out).view()
    } else if (params.input_file.endsWith('.csv')) {
        def epibuilder = run_epibuilder(input_file_path, jar_path)
        copy_results(epibuilder.out).view()
    } else {
        error "Unsupported file type. Use .fasta or .csv"
    }
}

/*
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
process copy_results2 {
    input:
    path output_dir

    output:
    stdout

    script:
    """
    echo "starting copy"
    echo "$output_dir"
    realpath ${output_dir}
    echo "${params.basename}"
    echo "${PWD}"
    rm -rf "${params.basename}"
    mkdir -p "${params.basename}"
    cp -r \$(realpath ${output_dir})/* "${params.basename}/"
    echo "Your results are in \$(realpath ${params.basename})"

    """
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

    # Resolve path
    final_path=\$(realpath ${params.basename})

    # Remove old results
    rm -rf "\$final_path"
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


    echo "Your results are in \$final_path"
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
    path "bepipred_output/raw_output.csv", emit: output

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
    path "epibuilder-results", emit: out

    script:
    def args = []
    if (params.minLength)   args << "--min-length ${params.minLength}"
    if (params.maxLength)   args << "--max-length ${params.maxLength}"
    if (params.threshold)   args << "--threshold ${params.threshold}"
    if (params.search && params.search != 'none') {
        args << "--search ${params.search}"
        if (params.proteomes && params.proteomes != null) {
            args << "--proteomes '${params.proteomes}'"
        }
    }
    

    """
    java -jar ${epibuilder_jar} --input ${input_file} --format csv ${args.join(' ')} --output epibuilder-results
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
    def validAminoAcids = "ACDEFGHIKLMNPQRSTVWY".toSet()
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
    def valid = []
    def invalid = []

    def currentHeader = null
    def currentSeq = new StringBuilder()

    new File(fastaPath).eachLine { line ->
        if (line.startsWith(">")) {
            if (currentHeader != null) {
                def seq = currentSeq.toString().replaceAll("\\s", "").toUpperCase()
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

    if (currentHeader != null) {
        def seq = currentSeq.toString().replaceAll("\\s", "").toUpperCase()
        if (isValidSequence(seq)) {
            valid << [header: currentHeader, seq: seq]
        } else {
            invalid << [header: currentHeader, seq: seq]
        }
    }

    new File("proteins_valid.fasta").withWriter { w ->
        valid.each {
            w.println(it.header)
            w.println(it.seq)
        }
    }

    if (invalid) {
        new File("proteins_invalid.fasta").withWriter { w ->
            invalid.each {
                w.println(it.header)
                w.println(it.seq)
            }
        }
    }

    println "Valid sequences: ${valid.size()}, Invalid sequences: ${invalid.size()}"
}