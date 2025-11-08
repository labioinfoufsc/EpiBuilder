FROM eclipse-temurin:21-jdk

LABEL app="EpiBuilder - CLI"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Sao_Paulo

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    tzdata \
    docker.io libgomp1 && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /db && \
    curl -L -o /db/uniprot_sprot.fasta.gz https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz && \
    gzip -d /db/uniprot_sprot.fasta.gz && \
    mv /db/uniprot_sprot.fasta /db/uniprot.fasta

RUN mkdir -p /db && \
    curl -f -L -o /db/uniprot_release_notes.txt https://ftp.uniprot.org/pub/databases/uniprot/current_release/relnotes.txt

COPY db/* /db/

RUN curl -s https://get.nextflow.io | bash && \
    mv nextflow /usr/local/bin/nextflow && chmod +x /usr/local/bin/nextflow

RUN mkdir /epibuilder
COPY . /epibuilder
WORKDIR /epibuilder

RUN mkdir /fasta
RUN cp /epibuilder/pipeline/fasta/ebola.fasta /fasta/ebola.fasta

RUN chmod +x /epibuilder/pipeline/epibuilder.sh
RUN ln -s /epibuilder/pipeline/epibuilder.sh /usr/local/bin/epibuilder

WORKDIR /

#ENTRYPOINT ["epibuilder"]
