# EpiBuilder

## What is EpiBuilder?

**EpiBuilder** is scientific software for assembling, searching, and classifying linear B-cell epitopes, especially for vaccine research using proteome-wide approaches.

It runs as a self-contained web application inside a single Docker container (monolith), which includes:

- A graphical user interface (frontend)
- Analysis and processing logic (backend)
- Workflow with NextFlow to use BepiPred 3.0 and BLAST
- A database (MySQL/MariaDB) to persist users and task data

## Requirements

- [Docker](https://www.docker.com/) must be installed on your computer.
  - No need to install programming languages, databases, or libraries separately.
  - Suitable for use on personal machines, lab computers, or servers.

## Downloading the Docker Image

Choose the appropriate version based on your system:

- **For systems with a compatible NVIDIA GPU (Ubuntu-based):**

```bash
docker pull bioinfoufsc/epibuilder:ubuntu-gpu
````

* **For standard systems without a GPU (Debian-based):**

```bash
docker pull bioinfoufsc/epibuilder:debian-cpu
```

## Running the Docker Container

### 1. Running the CPU version (Debian-based)

```bash
docker run -p 80:80 \
  -e FRONTEND_PORT=80 \
  -e BACKEND_PORT=8080 \
  -e DB_PORT=3306 \
  bioinfoufsc/epibuilder:debian-cpu
```

### 2. Running the GPU version (Ubuntu-based, requires NVIDIA drivers)

```bash
docker run -it --gpus all -p 80:80 \
  -e FRONTEND_PORT=80 \
  -e BACKEND_PORT=8080 \
  -e DB_PORT=3306 \
  bioinfoufsc/epibuilder:ubuntu-gpu
```

> **Note:** The GPU version should only be used if your system has a compatible NVIDIA GPU and the necessary drivers installed.

## Accessing the Web Interface

Once the container is running, open your web browser and go to:

```
http://localhost
```

The EpiBuilder web interface should load automatically.

## Login Credentials

* **Admin User**

Username: `admin`

Password: `admin`

> **Note:** An admin user has permission to create other users.