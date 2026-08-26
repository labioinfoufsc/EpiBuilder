FROM eclipse-temurin:21-jdk

LABEL app="EpiBuilder"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Sao_Paulo

# ============================================================
# SYSTEM DEPENDENCIES
# ============================================================

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg2 \
    tzdata \
    ncbi-blast+ \
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

# ============================================================
# NODE.JS + ANGULAR
# ============================================================

RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get update \
    && apt-get install -y nodejs \
    && npm install -g @angular/cli \
    && rm -rf /var/lib/apt/lists/*

# ============================================================
# DATABASE FILES
# ============================================================

RUN mkdir -p /tmp/epibuilder/db

COPY src/web/db/ /tmp/epibuilder/db/

RUN aria2c \
      --continue=true \
      --max-connection-per-server=8 \
      --min-split-size=2M \
      -d /tmp/epibuilder/db \
      https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz \
    && gunzip -f /tmp/epibuilder/db/uniprot_sprot.fasta.gz \
    && DATE=$(date +%Y_%m_%d) \
    && mv \
      /tmp/epibuilder/db/uniprot_sprot.fasta \
      /tmp/epibuilder/db/uniprot_sprot_${DATE}.fasta

# ============================================================
# BACKEND BUILD
# ============================================================

RUN mkdir -p /epibuilder/core
RUN mkdir -p /epibuilder/backend
RUN mkdir -p /epibuilder/frontend

# ----------------------------
# Copy pom.xml first (cache)
# ----------------------------

COPY src/core/pom.xml /epibuilder/core/pom.xml
COPY src/web/backend/pom.xml /epibuilder/backend/pom.xml

# ----------------------------
# Download Maven dependencies
# ----------------------------

WORKDIR /epibuilder/core

RUN mvn -B -q -DskipTests dependency:go-offline

# ----------------------------
# Copy full backend sources
# ----------------------------

COPY src/core/ /epibuilder/core/
COPY src/web/backend/ /epibuilder/backend/

# ----------------------------
# Build core
# ----------------------------

WORKDIR /epibuilder/core

RUN mvn clean install -DskipTests

# ----------------------------
# Build backend
# ----------------------------

WORKDIR /epibuilder/backend

RUN mvn clean package -DskipTests

# ----------------------------
# Validate generated jar
# ----------------------------

RUN jar tf target/epibuilder-backend.jar | grep EpibuilderBackendApplication

# ----------------------------
# Copy final backend jar
# ----------------------------

RUN cp \
    /epibuilder/backend/target/epibuilder-backend.jar \
    /epibuilder/epibuilder-backend.jar

# ============================================================
# FRONTEND BUILD
# ============================================================

# ----------------------------
# Copy package metadata first
# ----------------------------

COPY src/web/frontend/package*.json /epibuilder/frontend/

WORKDIR /epibuilder/frontend

# ----------------------------
# Install dependencies
# ----------------------------

RUN npm ci

# ----------------------------
# Copy frontend source
# ----------------------------

COPY src/web/frontend/ /epibuilder/frontend/

# ----------------------------
# Build Angular frontend
# ----------------------------

RUN npm run build

# ----------------------------
# Deploy frontend to nginx
# ----------------------------

RUN rm -rf /usr/share/nginx/html/* \
    && cp -r /epibuilder/frontend/dist/frontend/browser/* /usr/share/nginx/html/

# ============================================================
# NGINX
# ============================================================

RUN rm -f /etc/nginx/sites-enabled/default

COPY src/web/frontend/nginx.conf /etc/nginx/sites-enabled/default

# ============================================================
# ENTRYPOINT
# ============================================================

COPY src/web/bin/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

# ============================================================
# PORTS
# ============================================================

EXPOSE 80
EXPOSE 8080
EXPOSE 5432
EXPOSE 5005

# ============================================================
# WORKDIR
# ============================================================

WORKDIR /

# ============================================================
# ENTRYPOINT
# ============================================================

ENTRYPOINT ["/entrypoint.sh"]