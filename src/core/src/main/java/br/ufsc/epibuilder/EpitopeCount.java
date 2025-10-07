/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder;

/**
 *
 * @author renato
 */
public class EpitopeCount {

    private String ids;
    private int totalhits;
    private int totalProteins;

    public EpitopeCount(String ids, int total, int totalProteins) {
        this.ids = ids;
        this.totalhits = total;
        this.totalProteins = totalProteins;
    }

    public int getTotalProteins() {
        return totalProteins;
    }

    public void setTotalProteins(int totalProteins) {
        this.totalProteins = totalProteins;
    }
    

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String getIds() {
        return ids;
    }

    public void setTotalhits(int totalhits) {
        this.totalhits = totalhits;
    }

    public int getTotalhits() {
        return totalhits;
    }
    
}
