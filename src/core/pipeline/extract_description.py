import argparse
import os
import sys
import re
from Bio import SeqIO

def parse_fasta_to_tsv(fasta_file, output_path):
    """
    Reads a protein FASTA file, extracts ID and description using specific
    trimming rules, and saves the data to a TSV file at the specified path.

    Args:
        fasta_file (str): The path to the input FASTA file.
        output_path (str): The full path and filename for the output TSV file.
    """

    # 1. Determine the output directory from the full output path
    # os.path.dirname() extracts the directory component (e.g., 'output/proteins.tsv' -> 'output')
    output_dir = os.path.dirname(output_path)

    # If the user only provided a filename (e.g., 'data.tsv'), dirname returns an empty string.
    # We default the directory to the current path '.' in this case.
    if not output_dir:
        output_dir = '.'

    # 2. Create the output directory if it doesn't exist
    try:
        # os.makedirs(..., exist_ok=True) creates intermediate directories if needed
        # and doesn't raise an error if the directory already exists.
        os.makedirs(output_dir, exist_ok=True)
        print(f"Output directory ensured: '{output_dir}/'")
    except OSError as e:
        print(f"Error creating directory '{output_dir}': {e}", file=sys.stderr)
        return

    print(f"Processing file: {fasta_file}")

    try:
        with open(output_path, 'w') as outfile:
            # Write the TSV header
            outfile.write("ID\tDescription\n")

            for record in SeqIO.parse(fasta_file, "fasta"):
                protein_id = record.id
                full_header = record.description.strip()

                # Get the raw description by removing the ID from the full header
                if full_header.startswith(protein_id):
                    raw_description = full_header[len(protein_id):].strip()
                else:
                    raw_description = full_header

                final_description = raw_description

                # --- 1. PRIORITY RULE: Extract text between gene_product and transcript_product ---
                match_gp = re.search(r'gene_product=(.*?) \| transcript_product=', final_description)
                if match_gp:
                    final_description = match_gp.group(1).strip()
                else:
                    # If not found, apply the other trimming rules

                    # --- 2. RULE: Cut at ' OS=' ---
                    if ' OS=' in final_description:
                        final_description = final_description.split(' OS=')[0].strip()

                    # --- 3. RULE: Cut before ' [' (for organism/source in brackets) ---
                    match_bracket = re.search(r'\s\[', final_description)
                    if match_bracket:
                        final_description = final_description[:match_bracket.start()].strip()

                # Replace tabs in the final description with spaces
                final_description = final_description.replace('\t', ' ')

                # Write the line to the TSV: <id> <tab> <description>
                outfile.write(f"{protein_id}\t{final_description}\n")

        print(f"Success! Data saved to: {output_path}")

    except FileNotFoundError:
        print(f"Error: Input file not found at {fasta_file}", file=sys.stderr)
    except Exception as e:
        print(f"An error occurred during processing: {e}", file=sys.stderr)

if __name__ == "__main__":
    # Get the base name of the script to use in the default output file name
    # e.g., if input is 'yeast.fasta', default output is 'yeast/proteins.tsv'

    parser = argparse.ArgumentParser(
        description="Extracts IDs and descriptions from a protein FASTA file, applying specific trimming rules, and saves them to a TSV."
    )

    # Required argument: the FASTA file path
    parser.add_argument(
        "fasta_file",
        type=str,
        help="Path to the input protein FASTA file (e.g., .fasta, .fa)"
    )

    # Optional argument: the output file path (directory + filename)
    # The default value is calculated dynamically based on the input filename
    # However, since positional arguments are parsed first, we must parse once,
    # then check the default if no output is provided.

    # Note: We use a temporary default 'None' here, and calculate the actual default after parsing.
    parser.add_argument(
        "-o", "--output-file",
        type=str,
        default=None,
        help="Full path and filename for the output TSV file (e.g., 'results/proteins.tsv'). Defaults to <BASENAME>/proteins.tsv."
    )

    # Execute the parser
    args = parser.parse_args()

    # --- Dynamic Default Output Path Calculation ---
    output_path = args.output_file
    if output_path is None:
        # Calculate the default path if the user didn't provide one
        basename = os.path.splitext(os.path.basename(args.fasta_file))[0]
        output_path = os.path.join(basename, "proteins.tsv")

    # Call the main function
    parse_fasta_to_tsv(args.fasta_file, output_path)