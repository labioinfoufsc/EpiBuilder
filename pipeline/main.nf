#!/usr/bin/env nextflow

nextflow.enable.dsl=2

workflow {
    run_epibuilder_core()
    run_bepipred3(run_epibuilder_core.out)
    run_algpred2(run_epibuilder_core.out)
}

process run_epibuilder_core {
    tag 'epibuilder-core'

    input:
    path 'input.fasta'

    output:
    path 'raw_output.csv'

    script:
    """
    java -jar /epibuilder/epibuilder-core.jar input.fasta raw_output.csv
    """
}

process run_bepipred3 {
    tag 'bepipred3'

    input:
    path 'raw_output.csv'

    output:
    path 'bepipred_output.csv'

    script:
    """
    python /bepipred3/bepipred_predict.py --input raw_output.csv --output bepipred_output.csv
    """
}

process run_algpred2 {
    tag 'algpred2'

    input:
    path 'raw_output.csv'

    output:
    path 'algpred2_output.csv'

    script:
    """
    python /algpred2/algpred2_predict.py --input raw_output.csv --output algpred2_output.csv
    """
}