package br.ufsc.epibuilder.entity;

import br.ufsc.epibuilder.converter.ProteinConverter;
import br.ufsc.epibuilder.proteomics.FastaAdjust;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author renato
 */
public class Proteome {

    private String organism;
    private ArrayList<ProteinConverter> proteins = new ArrayList<>();
    private File file;
    private boolean load = false;

    public void load() throws Exception {
        if (!load) {
            proteins = new ArrayList<>();
            /*LinkedHashMap<String, ProteinSequence> a = FastaReaderHelper.readFastaProteinSequence(file);

        for (Map.Entry<String, ProteinSequence> entry : a.entrySet()) {
            proteins.add(new ProteinConverter(entry.getValue().getAccession().getID(), entry.getValue().getSequenceAsString()));
        }*/

            proteins.addAll(FastaAdjust.getProteins(file));
            load = true;
        }
    }

    public Proteome(String organism) {
        this.organism = organism;
    }

    public Proteome(String organism, ArrayList<ProteinConverter> proteins) {
        this.organism = organism;
        this.proteins = proteins;
    }

    public Proteome(String organism, File file) {
        this.organism = organism;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setProteins(ArrayList<ProteinConverter> proteins) {
        this.proteins = proteins;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getOrganism() {
        return organism;
    }

    public ArrayList<ProteinConverter> getProteins() {
        return proteins;
    }

    @Override
    public String toString() {
        return organism + "=" + file.getAbsolutePath();
    }

   /* public void clear() {
        proteins = new ArrayList<>();
    }*/

}
