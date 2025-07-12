/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity;

public class ReportBCell {

    private int position;
    private String residue;
    private int startPos;
    private int endPos;
    private String peptide;
    private float score;

    public ReportBCell(int position, String residue, int startPos, int endPos, String peptide, float score) {
        this.position = position;
        this.residue = residue;
        this.startPos = startPos;
        this.endPos = endPos;
        this.peptide = peptide;
        this.score = score;
    }

    /**
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * @return the residue
     */
    public String getResidue() {
        return residue;
    }

    /**
     * @param residue the residue to set
     */
    public void setResidue(String residue) {
        this.residue = residue;
    }

    /**
     * @return the startPos
     */
    public int getStartPos() {
        return startPos;
    }

    /**
     * @param startPos the startPos to set
     */
    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    /**
     * @return the endPos
     */
    public int getEndPos() {
        return endPos;
    }

    /**
     * @param endPos the endPos to set
     */
    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }

    /**
     * @return the peptide
     */
    public String getPeptide() {
        return peptide;
    }

    /**
     * @param peptide the peptide to set
     */
    public void setPeptide(String peptide) {
        this.peptide = peptide;
    }

    /**
     * @return the score
     */
    public float getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(float score) {
        this.score = score;
    }

}
