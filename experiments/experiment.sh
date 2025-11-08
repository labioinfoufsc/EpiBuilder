#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="${PWD}"
DB_DIR="${BASE_DIR}/db/proteomes"
EPIBUILDER_IMAGE="bioinfoufsc/epibuilder-core"
EPIBUILDER_VOLUME="epibuilder-data"
MIN_LENGTH=10
MAX_LENGTH=30
THRESHOLD=0.1512
COVER=80
IDENTITY=80

mkdir -p "${DB_DIR}"
docker pull "${EPIBUILDER_IMAGE}"

# -------------------------------
# Function to download proteome via Uniprot
# -------------------------------
download_proteome() {
    local accession="$1"
    local group="$2"

    local dir="${DB_DIR}/${group}"
    mkdir -p "$dir"
    local out_file="${dir}/${accession}.fasta"  

    echo "⬇️  Downloading proteome $accession -> $out_file"
    curl -s "https://rest.uniprot.org/uniprotkb/stream?format=fasta&query=proteome:${accession}" -o "$out_file"
}


# -------------------------------
# Function to process the biological group 
# -------------------------------
process_group() {
    local group_name="$1"
    declare -n accession_map="$2"
    local loc="$3"

    echo "🔹 Processing group $group_name (loc=$loc)"

    GROUP_DIR="${DB_DIR}/${group_name}"
    mkdir -p "$GROUP_DIR"

    # Download all group proteome
    for acc in "${!accession_map[@]}"; do
        clean_name="${accession_map[$acc]}"
        download_proteome "$acc" "$group_name" "$clean_name"
    done

    # Define the first proteome as the main
    FIRST_PROTEOME=$(ls "$GROUP_DIR"/*.fasta | head -n 1)

    # Mount proteome db args
    PROTEOMES_ARG="iedb=/db/iedb.fasta:uniprot=/db/uniprot.fasta"
    for acc in "${!accession_map[@]}"; do
        clean_name="${accession_map[$acc]}"
        PROTEOMES_ARG="${PROTEOMES_ARG}:${clean_name}=/experiment/db/proteomes/${group_name}/${acc}.fasta"
    done

    # Execute the docker with epibuilder
    OUTPUT_DIR="${BASE_DIR}/${group_name}/epibuilder_results"
    mkdir -p "$OUTPUT_DIR"

    CMD="docker run -it --rm \
        -v ${BASE_DIR}:/experiment/ \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v ${EPIBUILDER_VOLUME}:/tmp/epibuilder \
        -e EPIBUILDER_VOLUME=${EPIBUILDER_VOLUME} \
        ${EPIBUILDER_IMAGE} epibuilder \
        --input_file /experiment/db/proteomes/${group_name}/$(basename "$FIRST_PROTEOME") \
        --loc ${loc} \
        --min-length ${MIN_LENGTH} \
        --max-length ${MAX_LENGTH} \
        --threshold ${THRESHOLD} \
        --proteomes ${PROTEOMES_ARG} \
        --output /experiment/${group_name}/ \
        --cover ${COVER} \
        --identity ${IDENTITY} --bepipred_batch 10000"

    echo "▶ Running: $CMD"
    eval "$CMD"
}

# -------------------------------
# Map the proteomes x experiment
# -------------------------------
declare -A BORRELIA=(
    ["UP000001807"]="B_burgdorferi_B31"
    ["UP000005216"]="B_afzelii_PKo"
    ["UP000274630"]="B_garinii_PBi"
    ["UP000230633"]="B_miyamotoi_Yekat1"
    ["UP000000611"]="B_duttonii_Ly"
    ["UP000000612"]="B_recurrentis_A1"
    ["UP000019337"]="B_crocidurae_DOU"
    ["UP000019331"]="B_parkeri_SLO"
    ["UP000019269"]="B_nietonii_YOR"
    ["UP000326393"]="B_maritima_CA690"
    ["UP000078430"]="B_hermsii_HS1"
    ["UP000275571"]="B_turcica_IST7"
    ["UP001317516"]="C_B_fainii_Qtaro"
    ["UP000019330"]="B_coriaceae_Co53"
    ["UP000185502"]="B_anserina_Es"
    ["UP001305787"]="B_andersonii_21038"
    ["UP000001634"]="B_bissettiae_DN127"
)

declare -A EBOLA=(
    ["UP000007209"]="Zaire_ebolavirus"
    ["UP000000277"]="Sudan_ebolavirus"
    ["UP000180448"]="LakeVictoria_marburgvirus"
    ["UP000100390"]="TaiForest_ebolavirus"
    ["UP000007207"]="Reston_ebolavirus"
)

declare -A PLASMODIUM=(
    ["UP000001450"]="P_falciparum_3D7"
    ["UP000030673"]="P_falciparum_NF54"
    ["UP000031513"]="P_knowlesi_H"
    ["UP000219813"]="P_malariae"
    ["UP000078555"]="P_ovale_wallikeri"
    ["UP000242942"]="P_ovale_PocGH01"
    ["UP000008333"]="P_vivax_SalvadorI"
    ["UP000006319"]="P_cynomolgi_B"
    ["UP000030640"]="P_inui_SanAntonio1"
)

declare -A INFLUENZA=(
    ["UP000008158"]="InfluenzaB_BLee1940"
    ["UP000204142"]="InfluenzaD_bovineFrance2986"
    ["UP000235200"]="InfluenzaD_swineOklahoma1334"
    ["UP000008286"]="InfluenzaC_AnnArbor1"
    ["UP000009255"]="InfluenzaA_PuertoRico_H1N1"
    ["UP000131152"]="InfluenzaA_GooseGuangdong_H5N1"
)

declare -A STAPHYLOCOCCUS=(
    ["UP000008816"]="S_aureus"
    ["UP000000531"]="S_epidermidis"
    ["UP000600220"]="S_pseudintermedius"
    ["UP001269271"]="S_haemolyticus"
    ["UP000572988"]="S_schleiferi"
    ["UP000538955"]="S_capitis"
    ["UP000325462"]="S_lugdunensis"
    ["UP000006371"]="S_saprophyticus"
    ["UP000255549"]="S_intermedius"
)

declare -A CANDIDA=(
    ["UP000000559"]="C_albicans"
    ["UP000002037"]="C_tropicalis"
    ["UP000002428"]="C_glabrata"
    ["UP000005018"]="C_orthopsilosis"
    ["UP000005221"]="C_parapsilosis"
    ["UP000002605"]="C_dubliniensis"
    ["UP000230249"]="C_auris"
    ["UP000007703"]="C_lusitaniae"
    ["UP000011777"]="C_maltosa"
    ["UP000669133"]="C_metapsilosis"
)

# -------------------------------
# Execute all groups
# -------------------------------
process_group "INFLUENZA" INFLUENZA none
process_group "EBOLA" EBOLA none
process_group "BORRELIA" BORRELIA gram_neg
process_group "STAPHYLOCOCCUS" STAPHYLOCOCCUS gram_pos
process_group "PLASMODIUM" PLASMODIUM animal
process_group "CANDIDA" CANDIDA fungi

echo "✅ All groups processed!"
