FROM eclipse-temurin:21-jdk

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

RUN mkdir -p /tmp/epibuilder/db

COPY db/ /tmp/epibuilder/db/

RUN aria2c --continue=true --max-connection-per-server=8 --min-split-size=2M \
    -d /tmp/epibuilder/db \
    https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz && \
    gunzip -f /tmp/epibuilder/db/uniprot_sprot.fasta.gz && \
    DATE=$(date +%Y_%m_%d) && \
    mv /tmp/epibuilder/db/uniprot_sprot.fasta \
       /tmp/epibuilder/db/uniprot_sprot_${DATE}.fasta

# --- Instala NPM

# Instala Node.js v20 e npm (oficial NodeSource)
RUN apt-get update && apt-get install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g @angular/cli

# --- Instala NGINX mainline ---
RUN apt-get install -y --no-install-recommends nginx

# Limpa cache do apt
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Working directory
WORKDIR /opt/tools

# Copy application files
COPY core/ /epibuilder/core
COPY backend/ /epibuilder/backend

# Build core first
WORKDIR /epibuilder/core
RUN mvn clean install -DskipTests

# Later backend
WORKDIR /epibuilder/backend
RUN mvn clean package -DskipTests

# Move jar final
RUN mv target/*.jar /epibuilder/epibuilder-backend.jar

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
