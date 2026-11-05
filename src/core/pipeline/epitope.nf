#!/usr/bin/env nextflow

nextflow.enable.dsl = 2

// The pipeline accepts only two user parameters:
//   --input  Input FASTA file
//   --batch Number of sequences per batch
params.input = null
params.batch = null

// Keep the pipeline and epibuilder-core.jar in the same directory.
def epibuilderJar = file("${projectDir}/epibuilder-core.jar")
def resultsDir = "${launchDir}/epitope_results"

process validate_fasta {
    tag 'validate_fasta'

    input:
    path input_fasta

    output:
    path 'proteins_valid.fasta', emit: valid_proteins

    script:
    """
    #!/usr/bin/env bash
    set -euo pipefail

    java -cp "${epibuilderJar}" FastaValidation \
        --input "${input_fasta}" \
        --valid proteins_valid.fasta \
        --invalid proteins_invalid.fasta \
        --description description.tsv
    """
}

process run_epidope {
    tag 'epidope'

    publishDir resultsDir, mode: 'copy', overwrite: true, pattern: 'epidope.csv'

    input:
    path valid_fasta

    output:
    path 'epidope.csv', emit: csv
    path 'fasta_for_bepipred.fasta', emit: next_fasta

    script:
    """
    #!/usr/bin/env bash
    set -euo pipefail

    echo '[INFO] Running Epidope (batch=${params.batch})'
    # Assumes Epidope has the same -i/-o/-s CLI options as Bepipred3.
    java -cp "${epibuilderJar}" Epidope \
        -i "${valid_fasta}" \
        -o epidope.csv \
        -s ${params.batch}

    # Emit the validated FASTA only after Epidope succeeds, so that
    # run_bepipred cannot start before run_epidope finishes.
    cp -- "${valid_fasta}" fasta_for_bepipred.fasta
    """
}

process run_bepipred {
    tag 'bepipred'

    publishDir resultsDir, mode: 'copy', overwrite: true, pattern: 'bepipred.csv'

    input:
    path valid_fasta

    output:
    path 'bepipred.csv', emit: csv

    script:
    """
    #!/usr/bin/env bash
    set -euo pipefail

    echo '[INFO] Running BepiPred3 (batch=${params.batch}, CPU)'
    java -cp "${epibuilderJar}" Bepipred3 \
        -i "${valid_fasta}" \
        -o bepipred.csv \
        -s ${params.batch}
    """
}

workflow {
    if (!params.input) {
        error 'Required parameter missing: --input /path/to/file.fasta'
    }
    if (params.batch == null || !(params.batch.toString() ==~ /^[1-9][0-9]*$/)) {
        error 'Required parameter --batch must be a positive integer'
    }
    if (!new File("${projectDir}/epibuilder-core.jar").isFile()) {
        error "EpiBuilder JAR not found: ${epibuilderJar}"
    }

    def inputFasta = file(params.input, checkIfExists: true)
    if (!inputFasta.name.toLowerCase().endsWith('.fasta') &&
        !inputFasta.name.toLowerCase().endsWith('.fa') &&
        !inputFasta.name.toLowerCase().endsWith('.faa')) {
        error 'Input must be a FASTA file (.fasta, .fa or .faa)'
    }

    def validated = validate_fasta(inputFasta)
    def epidopeResult = run_epidope(validated.valid_proteins)
    run_bepipred(epidopeResult.next_fasta)
}
