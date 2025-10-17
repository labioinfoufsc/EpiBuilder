#!/usr/bin/env python3
import sys
import csv

if len(sys.argv) != 3:
    print(f"Usage: {sys.argv[0]} <input_localization.txt> <localization_output.tsv>")
    sys.exit(1)

input_file = sys.argv[1]
output_file = sys.argv[2]

# Mapeamento antigo -> novo
loc_map = {
    'cyts':'Cytoskeleton',
    'cyto':'Cytosol',
    'E.R.':'Endoplasmic Reticulum',
    'extr':'Extracellular',
    'golg':'Golgi apparatus',
    'mito':'Mitochondrion',
    'nucl':'Nucleus',
    'plas':'Plasma membrane',
    'pero':'Peroxisome',
    'vacu':'Vacuolar membrane',
    'chlo':'Chloroplast'
}

with open(input_file) as f_in, open(output_file, 'w', newline='') as f_out:
    writer = csv.writer(f_out, delimiter='\t')
    writer.writerow(['SeqID', 'Localization', 'Score'])

    next(f_in)
    for line in f_in:
        line = line.strip()
        if not line or line.startswith("#"):
            continue

        prediction = line.split(',')[0].strip()

        parts = prediction.split()
        seq_id = parts[0]
        score = parts[2]
        loc = loc_map.get(parts[1], 'Unknown')
        writer.writerow([seq_id, loc, score])
