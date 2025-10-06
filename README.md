🇺🇸 [EN](./README.md) | 🇪🇸 [ES](./README.es.md) | 🇫🇷 [FR](./README.fr.md) | 🇮🇹 [IT](./README.it.md) | 🇧🇷 [PT-BR](./README.pt-br.md)

# EpiBuilder

## What is EpiBuilder?

EpiBuilder is a scientific software for assembling, searching, and classifying linear B-cell epitopes, for research using approaches with partial or complete proteomes.

It runs as a self-contained web application inside a single Docker container (monolith), which includes:

- A graphical user interface (`frontend`)
- Analysis and processing logic (`backend` and `core`)
- Workflow with NextFlow to use BepiPred 3.0 and BLAST
- A database (PostgreSQL) to persist users and task data

## Who should use EpiBuilder?

The platform is designed for researchers and professionals working at the intersection of immunology, proteomics, and bioinformatics. It is ideal for anyone who needs to perform *in silico* analysis of epitopes to accelerate their research.

Key application areas include:
- **Infectious Diseases:** identify target epitopes in pathogens, accelerating the development of vaccines and diagnostic tests;
- **Oncology:** identify epitopes in tumor proteins, enabling the selection of precise targets for the development of immunotherapies and cancer vaccines;
- **Neuroscience:** predict epitopes in nervous system proteins, facilitating the search for autoantibody biomarkers for the diagnosis of neurodegenerative diseases.

## Requirements

- [Docker](https://www.docker.com/) must be installed on your computer.
  - No need to install programming languages, databases, or libraries separately.
  - Suitable for use on personal machines, lab computers, or servers.

## Step 1: Download the Docker Image (Only Once)

Run this command only once to download the EpiBuilder image:

- **If your system has an NVIDIA GPU and drivers (Ubuntu-based):**

```bash
docker pull bioinfoufsc/epibuilder:ubuntu-gpu
````
> **Note:** You must have NVIDIA GPU drivers installed to run this GPU-based Docker container.  
> If you're using Linux and want to use EpiBuilder with GPU support, please make sure you have CUDA installed:  
> [https://docs.nvidia.com/cuda/cuda-installation-guide-linux](https://docs.nvidia.com/cuda/cuda-installation-guide-linux)


* **If your system does not have a NVIDIA GPU (Debian-based):**

```bash
docker pull bioinfoufsc/epibuilder:debian-cpu
```

> **Tip:** If unsure, use the CPU version.

## Step 2: Create and Start the EpiBuilder Container (Only Once)

Run the command below **only once** to create the container. This will also start it.

### Debian (CPU)

```bash
docker run -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  bioinfoufsc/epibuilder:debian-cpu
```
Or
### Ubuntu (GPU)

```bash
docker run --gpus all -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5432:5432 \
  bioinfoufsc/epibuilder:ubuntu-gpu
```

> **Tip:** The `--name epibuilder` option ensures the container is reusable.

## Step 3: Access the Web Interface

After starting the container, open your browser and go to:

```
http://localhost
```

You should see the EpiBuilder web interface.

## Step 4: Reusing the Container (Next Times)

You do **not** need to run `docker run` again.

To start the container via Terminal or command-line interface (CLI):

```bash
docker start epibuilder
```

To stop the container via Terminal or command-line interface (CLI):

```bash
docker stop epibuilder
```

## Login Credentials

Use the following to log in for the first time:

* **Username:** `admin`
* **Password:** `admin`

> **Note:** The admin account can create other users.
