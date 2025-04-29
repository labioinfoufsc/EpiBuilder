/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.udesc.cav.tese.glycosylation;

/**
 *
 * @author renato
 */
public class Motif {

    private String sequence;
    private int start;
    private int end;

    public Motif(String motif, int start, int end) {
        this.sequence = motif;
        this.start = start;
        this.end = end;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    @Override
    public String toString() {
        return String.format("%s[%s:%s]", sequence, start, end);
    }

}
