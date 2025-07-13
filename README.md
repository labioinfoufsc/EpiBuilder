## Overview

EpiBuilder is a tool for assembling, searching, and classifying linear B-cell epitopes. It is designed to assist in bioinformatics workflows for the analysis and management of biological data, focusing on linear B-cell epitope prediction and classification.

## Requirements

- [Docker](https://www.docker.com/) installed on your system.
  
## Docker Images

To pull the Docker image for the appropriate environment, use the following commands:

- For the **Ubuntu GPU** version:

```bash
docker pull bioinfoufsc/epibuilder:ubuntu-gpu
```
- For the **Debian CPU** version:

```bash
docker pull bioinfoufsc/epibuilder:debian-cpu
```
## Running the Docker Containers

### Debian-based Docker Container

To run the EPIBuilder Docker container with a Debian base, use the following command:

```bash
docker run -it \
  -p 80:80 \
  -p 8080:8080 \
  bioinfoufsc/epibuilder:debian-cpu
```

### Ubuntu-based Docker Container

To run the EPIBuilder Docker container with an Ubuntu base, use the following command:

```bash
docker run -it \
  -p 80:80 \
  -p 8080:8080 \
  bioinfoufsc/epibuilder:ubuntu-gpu
```

### Accessing the Web Interface

Once the container is running, you can access the web interface by opening your browser and navigating to:

```
http://localhost
```

### Login Credentials

- **Admin User**:  
  Username: `admin`  
  Password: `admin`
  
