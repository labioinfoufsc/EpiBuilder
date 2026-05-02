FROM eclipse-temurin:21-jdk

LABEL app="EpiBuilder - CLI"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Sao_Paulo

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    ncbi-blast+ \
    tzdata \
    docker.io \
    curl \
    aria2 \
    gzip \
    libgomp1 && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /db

COPY db/ /db/

RUN aria2c --continue=true \
    --max-connection-per-server=8 \
    --min-split-size=2M \
    -d /db \
    https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz && \
    gunzip -f /db/uniprot_sprot.fasta.gz && \
    DATE=$(date +%Y_%m_%d) && \
    mv /db/uniprot_sprot.fasta /db/uniprot_sprot_${DATE}.fasta

RUN curl -f -L \
    -o /db/uniprot_release_notes.txt \
    https://ftp.uniprot.org/pub/databases/uniprot/current_release/relnotes.txt

RUN curl -s https://get.nextflow.io | bash && \
    mv nextflow /usr/local/bin/nextflow && \
    chmod +x /usr/local/bin/nextflow

RUN mkdir -p /epibuilder

COPY . /epibuilder

WORKDIR /epibuilder

RUN mkdir -p /fasta && \
    cp /epibuilder/pipeline/fasta/ebola.fasta /fasta/ebola.fasta

RUN chmod +x /epibuilder/pipeline/epibuilder.sh && \
    ln -s /epibuilder/pipeline/epibuilder.sh /usr/local/bin/epibuilder

WORKDIR /