/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.udesc.cav.tese.glycosylation;

import br.ufsc.epibuilder.entity.report.FormatHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;

/**
 *
 * @author renato Asparagina (ASN) - N + Qualquer aminoacido menos prolina (Xaa)
 * P + Serina/Threonine(Ser/Thr) S/T
 */
public class GlycosylationFinder {

    public enum Type {
        SQL, TXT
    }

    public static boolean isNGlycMotif(String proteina) {
        boolean positive = false;
        for (int i = 0; i < proteina.length(); i++) {
            if ((i + 3) <= proteina.length()) {
                char pos1 = proteina.charAt(i);
                if (pos1 == 'N') {
                    String seq = proteina.substring(i, i + 3);
                    if (seq.charAt(1) != 'P' && (seq.endsWith("S") || seq.endsWith("T"))) {
                        positive = true;

                        i += 3;
                    }
                }
            }

        }
        return positive;
    }

    public static ArrayList<Motif> getNGlycMotif(String proteina) {
        // System.out.println("ID\tMotif\tStart\tEnd");
        ArrayList<Motif> motifs = new ArrayList<>();
        for (int i = 0; i < proteina.length(); i++) {
            if ((i + 3) <= proteina.length()) {
                char pos1 = proteina.charAt(i);
                if (pos1 == 'N') {
                    String seq = proteina.substring(i, i + 3);
                    if (seq.charAt(1) != 'P' && (seq.endsWith("S") || seq.endsWith("T"))) {
                        motifs.add(new Motif(seq, (i + 1), (i + 3)));
                    }
                }
            }
        }
        return motifs;
    }

    public static String getNGlycMotif(String id, String proteina, Type type) {
        // System.out.println("ID\tMotif\tStart\tEnd");
        StringBuffer res = new StringBuffer();
        boolean positive = false;
        for (int i = 0; i < proteina.length(); i++) {
            if ((i + 3) <= proteina.length()) {
                char pos1 = proteina.charAt(i);
                if (pos1 == 'N') {
                    String seq = proteina.substring(i, i + 3);
                    if (seq.charAt(1) != 'P' && (seq.endsWith("S") || seq.endsWith("T"))) {
                        positive = true;
                        if (type == Type.TXT) {
                            res.append(id + "\t" + seq + "\t" + (i + 1) + "\t" + (i + 3));
                            res.append("\n");

                        } else {
                            res.append(String.format("INSERT INTO NGLYC VALUES (NULL, '%s', '%s', %s, %s);", id, seq, (i + 1), (i + 3)));
                            res.append("\n");

                        }
                        i += 3;
                    }
                }
            }

        }
        if (positive) {
            return res.toString();
        } else {
            return "";
        }
    }

    public static void main1(String[] args) {
        System.out.println(isNGlycMotif("ICKQRGNPSATRDSTGVNNPT"));
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        StringBuilder sb = new StringBuilder();
        System.out.print("#Nglyc created by Renato Simoes renatosm@gmail.com\n");
        System.out.print("#Search pattern Ans + Xaa (Except P) + (Ser/Thr)\n");
        System.out.print("#ID\tMotif\tStart\tEnd\n");

        String file = "/tese/resultados/fasta/Tevansi.fasta";
        LinkedHashMap<String, ProteinSequence> a = FastaReaderHelper.readFastaProteinSequence(new File(file));

        for (Map.Entry<String, ProteinSequence> entry : a.entrySet()) {
            ArrayList<Motif> motivos = getNGlycMotif(entry.getValue().getSequenceAsString());
            String delim = "-";

            //Ï= String res = Joiner.on(delim).join(motivos);
            System.out.println("Motivos para " + entry.getValue().getAccession().getID() + "\t" + FormatHelper.getListAsString(delim, motivos));
//            for (Motif motivo : motivos) {
//                System.out.println(motivo.getSequence()+"\t"+motivo.getStart()+ "\t"+motivo.getEnd());
//            }
        }

    }

}
