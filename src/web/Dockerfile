FROM eclipse-temurin:21-jdk

LABEL app="EpiBuilder"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Sao_Paulo

# Install system dependencies in a single layer
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg2 \
    tzdata \
    lsb-release \
    ca-certificates \
    curl \
    maven \
    aria2 \
    libgomp1 \
    nginx \
    docker.io \
    && ln -fs /usr/share/zoneinfo/$TZ /etc/localtime \
    && dpkg-reconfigure --frontend noninteractive tzdata \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js and Angular CLI
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get update && apt-get install -y nodejs \
    && npm install -g @angular/cli \
    && rm -rf /var/lib/apt/lists/*

# ------------------------
# DATABASE
# ------------------------

# Prepare database directory
RUN mkdir -p /tmp/epibuilder/db

COPY src/web/db/ /tmp/epibuilder/db/

# Download and prepare UniProt dataset
RUN aria2c --continue=true --max-connection-per-server=8 --min-split-size=2M \
    -d /tmp/epibuilder/db \
    https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz \
    && gunzip -f /tmp/epibuilder/db/uniprot_sprot.fasta.gz \
    && DATE=$(date +%Y_%m_%d) \
    && mv /tmp/epibuilder/db/uniprot_sprot.fasta \
       /tmp/epibuilder/db/uniprot_sprot_${DATE}.fasta

# ------------------------
# BACKEND BUILD
# ------------------------

WORKDIR /epibuilder

# Copy only pom files first to leverage Docker cache
COPY src/core/pom.xml core/pom.xml
COPY src/web/backend/pom.xml backend/pom.xml

# Pre-download Maven dependencies
WORKDIR /epibuilder/core
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy full source
COPY src/core/ core/
COPY src/web/backend/ backend/

# Build core module
WORKDIR /epibuilder/core
RUN mvn clean install -DskipTests

# Build backend module
WORKDIR /epibuilder/backend
RUN mvn clean package -DskipTests

# Move final jar to a stable location
RUN mv target/*.jar /epibuilder/epibuilder-backend.jar

# ------------------------
# FRONTEND BUILD
# ------------------------

# Cache npm dependencies
COPY src/web/frontend/package*.json /epibuilder/frontend/

WORKDIR /epibuilder/frontend

RUN npm ci

# Copy frontend source and build
COPY src/web/frontend/ /epibuilder/frontend/

RUN npm run build

# Deploy frontend to nginx
RUN rm -rf /usr/share/nginx/html/* \
    && cp -r dist/frontend/browser/* /usr/share/nginx/html/

# ------------------------
# NGINX + ENTRYPOINT
# ------------------------

WORKDIR /epibuilder

# Replace default nginx config
RUN rm -f /etc/nginx/sites-enabled/default

COPY src/web/frontend/nginx.conf /etc/nginx/sites-enabled/default

# Setup entrypoint
COPY src/web/bin/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

# Expose ports
EXPOSE 80 8080 5432

WORKDIR /

ENTRYPOINT ["/entrypoint.sh"]
