# Base image: Debian 11
FROM ubuntu:22.04

LABEL app="EpiBuilder"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Sao_Paulo

# Atualiza pacotes e instala dependências básicas
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg2 \
    tzdata \
    lsb-release \
    ca-certificates \
    curl \
    docker.io \
    maven \
    aria2 libgomp1 && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    dpkg-reconfigure --frontend noninteractive tzdata

RUN mkdir /db
COPY db/ /db/

RUN wget -P /tmp https://ftp.ncbi.nlm.nih.gov/blast/executables/blast+/LATEST/ncbi-blast-2.17.0+-x64-linux.tar.gz && \
    tar -xzf /tmp/ncbi-blast-2.17.0+-x64-linux.tar.gz -C /tmp && \
    mv /tmp/ncbi-blast-2.17.0+/bin/blastp /usr/local/bin && \
    mv /tmp/ncbi-blast-2.17.0+/bin/makeblastdb /usr/local/bin && \
    rm -rf /tmp/ncbi-blast-2.17.0+ /tmp/ncbi-blast-2.17.0+-x64-linux.tar.gz

RUN aria2c --continue=true --max-connection-per-server=8 --min-split-size=2M \
    -d /db https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz && \
    gzip -d /db/uniprot_sprot.fasta.gz && \
    DATE=$(date +%Y_%m_%d) && \
    mv /db/uniprot_sprot.fasta /db/uniprot_sprot_${DATE}.fasta && \
    makeblastdb -in /db/uniprot_sprot_${DATE}.fasta -dbtype prot -out /db/uniprot_sprot_${DATE}.fasta && \
    makeblastdb -in /db/iedb.fasta -dbtype prot -out /db/iedb.fasta


RUN wget -P /tmp https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz && \
    mkdir -p /usr/local/java && \
    tar -xzf /tmp/jdk-21_linux-x64_bin.tar.gz -C /usr/local/java && \
    rm /tmp/jdk-21_linux-x64_bin.tar.gz && \
    ln -sf /usr/local/java/jdk-21* /usr/local/java/default-java

ENV JAVA_HOME=/usr/local/java/default-java
ENV PATH=$JAVA_HOME/bin:$PATH

# --- Instala PostgreSQL 15 ---
RUN echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
    && wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O- | apt-key add - \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
       postgresql-15 \
       postgresql-contrib-15 \
       postgresql-client-15

# --- Instala NPM

# Instala Node.js v20 e npm (oficial NodeSource)
RUN apt-get update && apt-get install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g @angular/cli

# --- Instala NGINX mainline ---
RUN wget -qO - https://nginx.org/keys/nginx_signing.key | apt-key add - \
    && echo "deb http://nginx.org/packages/mainline/ubuntu $(lsb_release -cs) nginx" > /etc/apt/sources.list.d/nginx.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nginx

# Limpa cache do apt
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Working directory
WORKDIR /opt/tools

# Copy application files
COPY backend/ /epibuilder/backend

WORKDIR /epibuilder/backend

RUN mvn clean package -DskipTests

RUN mv /epibuilder/backend/target/epibuilder-backend.jar /epibuilder/epibuilder-backend.jar

WORKDIR /

COPY frontend/ /epibuilder/frontend

WORKDIR /epibuilder/frontend
RUN npm ci
RUN npm run build
RUN mkdir -p /var/www/html/
RUN cp -r dist/frontend/browser/* /usr/share/nginx/html/

# NGINX configuration
RUN rm -f /etc/nginx/sites-enabled/default
COPY frontend/nginx.conf /etc/nginx/sites-enabled/default 

# Entrypoint setup
COPY bin/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh 
# Ports
EXPOSE 80
EXPOSE 8080
EXPOSE 5432

WORKDIR /
ENTRYPOINT ["/entrypoint.sh"]
