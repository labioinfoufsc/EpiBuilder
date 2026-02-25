🇺🇸 [EN](./README.md) | 🇪🇸 [ES](./docs/README.es.md) | 🇫🇷 [FR](./docs/README.fr.md) | 🇮🇹 [IT](./docs/README.it.md) | 🇧🇷 [PT-BR](./docs/README.pt-br.md)

# EpiBuilder

![logo](https://github.com/labioinfoufsc/EpiBuilder/blob/main/src/web/frontend/src/assets/epibuilder-logo.png)

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

```bash
docker pull bioinfoufsc/epibuilder:latest
````

## Step 2: Create and Start the EpiBuilder Container (Only Once)

Run the command below **only once** to create the container. This will also start it.

### (CPU)

```bash
  docker run -it --name epibuilder \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v epibuilder-data:/tmp/epibuilder \
  -e EPIBUILDER_VOLUME=epibuilder-data \
  -e DOCKER_API_VERSION=1.44 \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  -p 5005:5005 \
  bioinfoufsc/epibuilder:latest
```

> **Tip 1:** The `--name epibuilder` option ensures the container is reusable.

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

## 📖 Citation

If you use **EpiBuilder** in your research, please cite our article:

> Moreira RS, Filho VB, Calomeno NA, Wagner G, Miletti LC.  
> **EpiBuilder: A Tool for Assembling, Searching, and Classifying B-Cell Epitopes.**  
> *Bioinformatics and Biology Insights*, 2022 May 11;16:11779322221095221.  
> [https://doi.org/10.1177/11779322221095221](https://doi.org/10.1177/11779322221095221)  
> PMID: [35571557](https://pubmed.ncbi.nlm.nih.gov/35571557/) — PMCID: PMC9102138

