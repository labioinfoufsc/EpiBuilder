# Base image: Debian 11
FROM bioinfoufsc/epibuilder-core:latest

LABEL app="EpiBuilder"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

# Atualiza pacotes e instala dependências básicas
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg2 \
    lsb-release \
    ca-certificates \
    curl

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
