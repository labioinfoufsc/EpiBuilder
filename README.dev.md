# README.dev.md

# Development Environment Setup

This document describes how to configure the EpiBuilder development environment.

# Requirements

Recommended OS:

- Ubuntu 22.04+
or
- Debian-based Linux distributions

Minimum recommended hardware:

- 16 GB RAM
- 4 CPU cores
- 30+ GB free disk space

# Architecture Overview

The project is divided into 3 main components:

| Component | Description |
|---|---|
| `src/core` | CLI application and Nextflow pipeline |
| `src/web/backend` | Spring Boot backend |
| `src/web/frontend` | Angular frontend |
| `src/bepipred3` | Docker image for BepiPred3 predictions |

Some bioinformatics tools are already installed inside Docker containers during image build, including:

- Nextflow
- BLAST+
- BepiPred3 runtime
- Java dependencies

Because of this, local installation of these tools is not required.

# 1. Backend Environment

Backend setup script location:

```text
src/web/backend/backend.sh
```

Run:

```bash
cd src/web/backend
chmod +x backend.sh
./backend.sh
```

This script installs:

- Java 21
- Maven
- Docker

The script also creates the PostgreSQL container used by the backend.

# 2. Frontend Environment

Frontend setup script location:

```text
src/web/frontend/frontend.sh
```

Run:

```bash
cd src/web/frontend
chmod +x frontend.sh
./frontend.sh
```

This installs:

- NVM
- Node.js
- Angular CLI

# 3. Development Databases

Before running the backend or pipeline locally, prepare the BLAST databases.

Run:

```bash
cd bin
chmod +x setup_dev.sh
./setup_dev.sh
```

The script will:

- create `/tmp/epibuilder/db`
- copy `iedb.fasta` from `src/core/db`
- download the latest UniProt FASTA

Expected result:

```text
/tmp/epibuilder/db/
├── iedb.fasta
├── uniprot_sprot_YYYY_MM_DD.fasta
```

# 4. Build Docker Images

Build script location:

```text
bin/build.sh
```

Run:

```bash
cd bin
chmod +x build.sh
./build.sh
```

The script can build:

- `bioinfoufsc/bepipred3`
- `bioinfoufsc/epibuilder-core`
- `bioinfoufsc/epibuilder`

The script automatically:

- compiles Maven projects inside Docker
- builds Docker images
- reuses the local Maven cache
- skips tests during web backend compilation

# 5. Build Details

## BepiPred3 Image

Docker image:

```text
bioinfoufsc/bepipred3
```

Path:

```text
src/bepipred3
```

---

## Core Image

Docker image:

```text
bioinfoufsc/epibuilder-core
```

Path:

```text
src/core
```

This image already includes:

- Nextflow
- BLAST+
- UniProt database
- IEDB database
- EpiBuilder CLI
- Bioinformatics dependencies

## Web Image

Docker image:

```text
bioinfoufsc/epibuilder
```

Path:

```text
src/web
```

This image includes:

- Spring Boot backend
- Angular frontend
- NGINX
- Development databases copied to `/tmp/epibuilder/db`

# 6. Running Backend Locally

Path:

```text
src/web/backend
```

Compile:

```bash
mvn clean install -DskipTests
```

Run:

```bash
mvn spring-boot:run
```

# 7. Running Frontend Locally

Path:

```text
src/web/frontend
```

Install dependencies:

```bash
npm i
```

Run Angular:

```bash
ng serve
```

Frontend default URL:

```text
http://localhost:4200
```

# 8. PostgreSQL

The backend setup script automatically creates a PostgreSQL container. Check running containers:

```bash
docker ps
```

# 9. JWT Secret

The backend requires a JWT secret. The backend setup script automatically generates it. 