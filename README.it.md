🇺🇸 [EN](./README.md) | 🇪🇸 [ES](./README.es.md) | 🇫🇷 [FR](./README.fr.md) | 🇮🇹 [IT](./README.it.md) | 🇧🇷 [PT-BR](./README.pt-br.md)

# EpiBuilder

## Cos'è EpiBuilder?

EpiBuilder è un software scientifico per l'assemblaggio, la ricerca e la classificazione di epitopi lineari dei linfociti B, per la ricerca che utilizza approcci con proteomi parziali o completi.

Funziona come un'applicazione web autonoma all'interno di un singolo container Docker (monolite), che include:

- Un'interfaccia utente grafica (`frontend`)
- Logica di analisi ed elaborazione (`backend` e `core`)
- Flusso di lavoro con NextFlow per utilizzare BepiPred 3.0 e BLAST
- Un database (PostgreSQL) per salvare i dati degli utenti e delle attività

## A chi è rivolto EpiBuilder?

La piattaforma è pensata per ricercatori e professionisti che lavorano all'intersezione tra immunologia, proteomica e bioinformatica. È ideale per chiunque abbia bisogno di eseguire analisi *in silico* di epitopi per accelerare la propria ricerca.

Le principali aree di applicazione includono:
- **Malattie Infettive:** identificare epitopi bersaglio nei patogeni, accelerando lo sviluppo di vaccini e test diagnostici;
- **Oncologia:** identificare epitopi nelle proteine tumorali, consentendo la selezione di bersagli precisi per lo sviluppo di immunoterapie e vaccini contro il cancro;
- **Neuroscienze:** prevedere epitopi nelle proteine del sistema nervoso, facilitando la ricerca di biomarcatori autoanticorpali per la diagnosi di malattie neurodegenerative.

## Requisiti

- [Docker](https://www.docker.com/) deve essere installato sul tuo computer.
  - Non è necessario installare separatamente linguaggi di programmazione, database o librerie.
  - Adatto per l'uso su macchine personali, computer di laboratorio o server.

## Passaggio 1: Scarica l'immagine Docker (una sola volta)

Esegui questo comando una sola volta per scaricare l'immagine di EpiBuilder:

- **Se il tuo sistema ha una GPU NVIDIA e i relativi driver (basato su Ubuntu):**

```bash
docker pull bioinfoufsc/epibuilder:ubuntu-gpu
````

> **Nota:** È necessario avere installato i driver della GPU NVIDIA per eseguire questo container Docker basato su GPU.
> Se utilizzi Linux e vuoi usare EpiBuilder con supporto GPU, assicurati di aver installato CUDA:
> [https://docs.nvidia.com/cuda/cuda-installation-guide-linux](https://docs.nvidia.com/cuda/cuda-installation-guide-linux)

  - **Se il tuo sistema non ha una GPU NVIDIA (basato su Debian):**

<!-- end list -->

```bash
docker pull bioinfoufsc/epibuilder:debian-cpu
```

> **Suggerimento:** In caso di dubbio, utilizza la versione per CPU.

## Passaggio 2: Crea e avvia il container di EpiBuilder (una sola volta)

Esegui il comando sottostante **una sola volta** per creare il container. Questo comando lo avvierà anche.

### Debian (CPU)

```bash
docker run -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  bioinfoufsc/epibuilder:debian-cpu
```

O

### Ubuntu (GPU)

```bash
docker run --gpus all -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5432:5432 \
  bioinfoufsc/epibuilder:ubuntu-gpu
```

> **Suggerimento:** L'opzione `--name epibuilder` garantisce che il container sia riutilizzabile.

## Passaggio 3: Accedi all'interfaccia Web

Dopo aver avviato il container, apri il browser e vai a:

```
http://localhost
```

Dovresti vedere l'interfaccia web di EpiBuilder.

## Passaggio 4: Riutilizzo del container (le volte successive)

**Non** è necessario eseguire nuovamente `docker run`.

Per avviare il container tramite Terminale o interfaccia a riga di comando (CLI):

```bash
docker start epibuilder
```

Per fermare il container tramite Terminale o interfaccia a riga di comando (CLI):

```bash
docker stop epibuilder
```

## Credenziali di accesso

Usa le seguenti credenziali per il primo accesso:

  - **Nome utente:** `admin`
  - **Password:** `admin`

> **Nota:** L'account amministratore può creare altri utenti.