/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.converter;

import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author renato
 */
public class ProteinConverter {

    private String id;
    private StringBuilder sequence = new StringBuilder();
    private TreeMap<Integer, Amino> aminoacidMap = new TreeMap<>();
    private int index=0;

    public ProteinConverter(String id, String sequence) {
        this.id = id;
        this.sequence = new StringBuilder(sequence);
    }

    public ProteinConverter(String id) {
        this.id = id;
    }

    public String getSequence() {
        return sequence.toString();
    }
    
    public String getSequenceFromMap(){
        String sequence = "";
        for (Amino value : aminoacidMap.values()) {
            sequence+=value.getAa();
        }
        return sequence;
    }
    public void addAmino(String aa, double value) {
        aminoacidMap.put(index++, new Amino(aa, value));
    }
    
    public void addAmino(int pos, String aa, double value) {
        aminoacidMap.put(pos, new Amino(aa, value));
    }

    public String getId() {
        return id;
    }

    public TreeMap<Integer, Amino> getAminoacidMap() {
        return aminoacidMap;
    }

    public int count(String peptide) {
        /*String str = sequence;

        String strFind = peptide;
        int count = 0, fromIndex = 0;
        while ((fromIndex = str.indexOf(strFind, fromIndex)) != -1) {
            count++;
            fromIndex++;
        }*/

        return StringUtils.countMatches(sequence, peptide);
    }

    public void append(String ln) {
        sequence.append(ln.trim());
    }

    public class Amino {

        private String aa;
        private double value;

        public Amino(String aa, double value) {
            this.aa = aa;
            this.value = value;
        }

        public String getAa() {
            return aa;
        }

        public double getValue() {
            return value;
        }

    }
}
