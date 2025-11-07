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

RUN mkdir /db
COPY db/ /db/

RUN aria2c --continue=true --max-connection-per-server=8 --min-split-size=2M \
    -d /db https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz && \
    gzip -d /db/uniprot_sprot.fasta.gz && \
    DATE=$(date +%Y_%m_%d) && \
    mv /db/uniprot_sprot.fasta /db/uniprot_sprot_${DATE}.fasta

# --- Instala NPM

# Instala Node.js v20 e npm (oficial NodeSource)
RUN apt-get update && apt-get install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g @angular/cli

# --- Instala NGINX mainline ---
RUN wget -qO - https://nginx.org/keys/nginx_signing.key | apt-key add - \
    && echo "deb http://nginx.org/packages/mainline/debian $(lsb_release -cs) nginx" > /etc/apt/sources.list.d/nginx.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nginx

# Limpa cache do apt
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Working directory
WORKDIR /opt/tools

# Copy application files
COPY backend/ /epibuilder/backend

WORKDIR /epibuilder/backend

#RUN mvn clean package -DskipTests

RUN mv /epibuilder/backend/epibuilder-backend.jar /epibuilder/epibuilder-backend.jar

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
