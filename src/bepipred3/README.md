# BepiPred - 3.0

**Prediction of potential B-cell epitopes from protein sequences**  

**Created by:** DTU Health Tech  
**Available at:** [https://services.healthtech.dtu.dk/services/BepiPred-3.0/](https://services.healthtech.dtu.dk/services/BepiPred-3.0/)

---

## Overview

BepiPred 3.0 is a computational tool designed to **predict potential B-cell epitopes** from protein sequences. It is developed by DTU Health Tech and widely used in immunoinformatics studies to identify regions in proteins that may elicit antibody responses.

This repository provides a **Dockerized version** of BepiPred 3.0, allowing easy installation and execution without manually handling dependencies.

---

## Docker Installation

To build the Docker image from scratch:

```bash
git clone https://github.com/labioinfoufsc/BepiPred3.git
cd BepiPred3
docker build -t bioinfoufsc/bepipred3:latest .
```

This will create a Docker image named `bioinfoufsc/bepipred3:latest` with all necessary dependencies.

---

## Usage

Run BepiPred 3.0 using Docker:

```bash
docker run -it --rm -v ${PWD}:/data bioinfoufsc/bepipred3:latest -i /data/example.fasta -o /data/results -pred vt_pred
```

- `-i /data/example.fasta` : path to your input FASTA file  
- `-o /data/results` : path to the output directory  
- `-pred vt_pred` : prediction mode (default BepiPred 3.0 mode)

Your results will be saved in the `results` folder inside your current working directory.

---

## Input

- Standard FASTA protein sequences  
- Multiple sequences can be included in one FASTA file

---

## Output

- Tab-delimited or CSV files containing predicted B-cell epitopes and scores  
- Output files are stored in the folder specified by `-o` (e.g., `/data/results`)  
- Each row represents a residue with its predicted epitope score

---

## Example

Assuming you have a FASTA file named `example.fasta` in your current directory:

```bash
docker run -it --rm -v ${PWD}:/data bioinfoufsc/bepipred3:latest -i /data/example.fasta -o /data/results -pred vt_pred
```

After execution, you will find prediction results in the `results` folder in your current directory.

---

## License and Citation

This repository is provided for **academic and research purposes**.  
Please cite the original BepiPred 3.0 publication when using this tool in your research:

Clifford JN, Høie MH, Deleuran S, Peters B, Nielsen M, Marcatili P. BepiPred-3.0: Improved B-cell epitope prediction using protein language models. *Protein Sci.* 2022 Dec;31(12):e4497. doi: 10.1002/pro.4497. PMID: 36366745; PMCID: PMC9679979.

For commercial use, please contact DTU Health Tech.

