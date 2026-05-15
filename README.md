🇺🇸 [EN](./README.md) | 🇪🇸 [ES](./docs/README.es.md) | 🇫🇷 [FR](./docs/README.fr.md) | 🇮🇹 [IT](./docs/README.it.md) | 🇧🇷 [PT-BR](./docs/README.pt-br.md)

# EpiBuilder

![logo](./src/web/frontend/src/assets/epibuilder-logo.png)

## What is EpiBuilder?

EpiBuilder is a scientific software for assembling, searching, and classifying linear B-cell epitopes, for research using approaches with partial or complete proteomes.

The software is designed as a multi-container application using Docker to execute a reproducible immunoinformatics workflow.

## Who should use EpiBuilder?

The platform is designed for researchers and professionals working at the intersection of immunology, proteomics, and bioinformatics. It is ideal for anyone who needs to perform *in silico* analysis of epitopes to accelerate their research and particularly suited for proteome-wide epitope discovery.

Key application areas include:

- **Infectious Diseases**: identify target epitopes in pathogens, accelerating the development of vaccines and diagnostic tests;
- **Oncology**: identify epitopes in tumor proteins, enabling the selection of precise targets for the development of immunotherapies and cancer vaccines;
- **Neuroscience**: predict epitopes in nervous system proteins, facilitating the search for autoantibody biomarkers for the diagnosis of neurodegenerative diseases.

---

## How EpiBuilder Works

EpiBuilder orchestrates a [Nextflow](https://www.nextflow.io/) pipeline that integrates the following dockerized tools:

- **[BepiPred-3.0](https://services.healthtech.dtu.dk/services/BepiPred-3.0/)** — deep learning-based B-cell epitope prediction from protein sequences;
- **[WolfPSORT](https://wolfpsort.hgc.jp/)** — subcellular localization prediction for eukaryotic organisms (`animal`, `fungi`, `plant`);
- **[PSORTb](https://www.psort.org/psortb/)** — subcellular localization prediction for prokaryotic organisms (`arch`, `gram_pos`, `gram_neg`);
- **[NCBI BLAST+](https://blast.ncbi.nlm.nih.gov/Blast.cgi)** — sequence similarity search against UniProt proteomes (optional, used when proteome filtering is enabled);
- **EpiBuilder Core** - custom Java modules for FASTA validation, epitope filtering, topology analysis, and Sheets export.

---

## Requirements

- [Docker](https://www.docker.com/) must be installed on your computer.
  - No need to install programming languages, databases, or libraries separately.
  - Suitable for use on personal machines, lab computers, or servers.
- Linux environment recommended.

---

## Usage Modes

EpiBuilder can be used in two ways depending on your needs:

| Mode | Description |
|------|-------------|
| **Web** | Graphical interface with database, user management, and result storage |
| **CLI** | Command-line execution, ideal for scripting and automation |

---

## Web Interface (Recommended)

### Step 1: Start EpiBuilder

Download the [`docker-compose.yml`](https://github.com/labioinfoufsc/EpiBuilder/blob/main/docker-compose.yml) file and run from the directory where it was saved:

```bash
docker compose up -d
```

This will automatically download all required images and start the application. The first run may take longer than usual since `bepipred3` docker image will be downloaded.

> For development setup and guidelines, see [README.dev.md](./README.dev.md)

### Step 2: Access the Web Interface

After starting, open your browser and go to:

```
http://localhost
```

### Step 3: Reusing (Next Times)

```bash
# Start
docker compose up -d

# Stop
docker compose down
```

### Login Credentials

Use the following to log in for the first time:

- **Username:** `admin`
- **Password:** `admin`

> **Note:** The admin account can create other users.

---

## CLI (Command-Line Interface)

The CLI mode runs epitope prediction directly from the terminal without a graphical interface or database.

### Step 1: Pull the images

```bash
docker pull bioinfoufsc/epibuilder-core:latest
docker pull bioinfoufsc/bepipred3:latest
```

> **Note:** If not present, `bepipred3` docker it will be downloaded automatically. For this reason, the first run may take longer than usual.

### Step 2: Run the analysis

```bash
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v epibuilder-data:/tmp/epibuilder \
  -e EPIBUILDER_VOLUME=epibuilder-data \
  bioinfoufsc/epibuilder-core:latest \
  epibuilder --input_file /tmp/epibuilder/your_file.fasta
```

Replace `/tmp/epibuilder/your_file.fasta` with the path to your FASTA file inside the mounted volume.

---

## Contributing

Contributions are welcome.

- Bug fixes, improvements, and new features must be submitted via Pull Request (PR).
- Please ensure your changes are tested before submission.
- Follow the existing project structure and conventions.

> For development setup and guidelines, see [README.dev.md](./README.dev.md)

---

## Citation

If you use EpiBuilder in your research, please cite:

> Moreira RS, Filho VB, Calomeno NA, Wagner G, Miletti LC.
> EpiBuilder: A Tool for Assembling, Searching, and Classifying B-Cell Epitopes.
> Bioinformatics and Biology Insights, 2022.
> https://doi.org/10.1177/11779322221095221
