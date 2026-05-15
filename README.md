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

## Requirements

- [Docker](https://www.docker.com/) must be installed on your computer.
  - No need to install programming languages, databases, or libraries separately.
  - Suitable for use on personal machines, lab computers, or servers.
- Linux environment recommended.

---

## Step 1: Create and Start EpiBuilder (Only Once)

Run the commands below **only once** to create and start all required containers.

### 1.1 — Database

```bash
docker run -d \
  --name epibuilder-db \
  -e POSTGRES_DB=epibuilder \
  -e POSTGRES_USER=epiuser \
  -e POSTGRES_PASSWORD=epiuser \
  -v epibuilder-pgdata:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:16
```

### 1.2 — Application

```bash
docker run -d \
  --name epibuilder-web \
  --add-host=host.docker.internal:host-gateway \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v epibuilder-data:/tmp/epibuilder \
  -e EPIBUILDER_VOLUME=epibuilder-data \
  -p 80:80 \
  -p 8080:8080 \
  -p 5005:5005 \
  bioinfoufsc/epibuilder:latest
```

> **Note:** The `epibuilder-core` and `bepipred3` images are pulled and managed automatically by the application when needed. No additional setup is required.

> For development setup and guidelines, see [README.dev.md](./README.dev.md)

---

## Step 2: Access the Web Interface

After starting the containers, open your browser and go to:

```
http://localhost
```

You should see the EpiBuilder web interface.

---

## Step 3: Reusing the Containers (Next Times)

You do **not** need to run `docker run` again. To start and stop the containers:

```bash
# Start
docker start epibuilder-db
docker start epibuilder-web

# Stop
docker stop epibuilder-web
docker stop epibuilder-db
```

---

## Login Credentials

Use the following to log in for the first time:

- **Username:** `admin`
- **Password:** `admin`

> **Note:** The admin account can create other users.

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
