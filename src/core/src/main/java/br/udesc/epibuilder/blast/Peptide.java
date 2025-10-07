/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.udesc.epibuilder.blast;

/**
 *
 * @author renato
 */
public class Peptide {
    private String peptideAccessId;
    private String queryaAccessId;
    private double identity;
    private double cover;

    public Peptide(String blastline){
        String[] tab = blastline.split("\t");
        peptideAccessId = tab[0];
        queryaAccessId = tab[1];
        identity = Double.parseDouble(tab[2]);
        cover = Double.parseDouble(tab[3]);
        
    }
    /**
     * @return the peptideAccessId
     */
    public String getPeptideAccessId() {
        return peptideAccessId;
    }

    /**
     * @param peptideAccessId the peptideAccessId to set
     */
    public void setPeptideAccessId(String peptideAccessId) {
        this.peptideAccessId = peptideAccessId;
    }

    /**
     * @return the queryaAccessId
     */
    public String getQueryaAccessId() {
        return queryaAccessId;
    }

    /**
     * @param queryaAccessId the queryaAccessId to set
     */
    public void setQueryaAccessId(String queryaAccessId) {
        this.queryaAccessId = queryaAccessId;
    }

    /**
     * @return the identity
     */
    public double getIdentity() {
        return identity;
    }

    /**
     * @param identity the identity to set
     */
    public void setIdentity(double identity) {
        this.identity = identity;
    }

    /**
     * @return the cover
     */
    public double getCover() {
        return cover;
    }

    /**
     * @param cover the cover to set
     */
    public void setCover(double cover) {
        this.cover = cover;
    }

    @Override
    public String toString() {
        return String.format("%s(%.2f,%.2f)",queryaAccessId, identity, cover);
    }
    
    
}
