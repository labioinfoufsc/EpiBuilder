FROM openjdk:21-jdk-bookworm

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

RUN curl -s https://get.nextflow.io | bash && \
    mv nextflow /usr/local/bin/nextflow && chmod +x /usr/local/bin/nextflow

RUN mkdir /epibuilder
COPY . /epibuilder
WORKDIR /epibuilder

RUN chmod +x /epibuilder/pipeline/epibuilder.sh
RUN chmod +x /epibuilder/pipeline/bepipred3.sh
RUN chmod +x /epibuilder/pipeline/localization.sh
RUN chmod +x /epibuilder/pipeline/blastp_custom.sh

RUN ln -s /epibuilder/pipeline/epibuilder.sh /usr/local/bin/epibuilder
RUN ln -s /epibuilder/pipeline/bepipred3.sh /usr/local/bin/bepipred3
RUN ln -s /epibuilder/pipeline/localization.sh /usr/local/bin/localization
RUN ln -s /epibuilder/pipeline/blastp_custom.sh /usr/local/bin/blastp_custom

WORKDIR /
