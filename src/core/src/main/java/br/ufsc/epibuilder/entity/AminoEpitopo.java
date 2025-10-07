/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity;


/**
 *
 * @author renato
 */
public class AminoEpitopo {
    private String amino;
    private int position;
    private double parker;
    private double chou_fasman;
    private double emini;
    private double karplus_schulz;
    private double kolaskar;
    private double bepipred2;
    private double kda;
    private double ip;

    public void setKda(double kda) {
        this.kda = kda;
    }

    public void setPi(double ip) {
        this.ip = ip;
    }

    public double getIp() {
        return ip;
    }

    public double getKda() {
        return kda;
    }
    
    
    
    /**
     * Retorna o valor do software, o padrao é bepipred
     * @param software
     * @return 
     */
    public double getValue(SoftwareBcellEnum software){
        switch (software) {
            case PARKER: return parker;
            case CHOU_FOSMAN: return chou_fasman;
            case EMINI: return emini;
            case KARPLUS_SCHULZ: return karplus_schulz;
            case KOLASKAR: return kolaskar;
            default: return bepipred2;
        }
    }
    /**
     * @return the parker
     */
    public double getParker() {
        return parker;
    }

    /**
     * @param parker the parker to set
     */
    public void setParker(double parker) {
        this.parker = parker;
    }

    /**
     * @return the chou_fasman
     */
    public double getChou_fasman() {
        return chou_fasman;
    }

    /**
     * @param chou_fasman the chou_fasman to set
     */
    public void setChou_fasman(double chou_fasman) {
        this.chou_fasman = chou_fasman;
    }

    /**
     * @return the emini
     */
    public double getEmini() {
        return emini;
    }

    /**
     * @param emini the emini to set
     */
    public void setEmini(double emini) {
        this.emini = emini;
    }

    /**
     * @return the karplus_schulz
     */
    public double getKarplus_schulz() {
        return karplus_schulz;
    }

    /**
     * @param karplus_schulz the karplus_schulz to set
     */
    public void setKarplus_schulz(double karplus_schulz) {
        this.karplus_schulz = karplus_schulz;
    }

    /**
     * @return the kolaskar
     */
    public double getKolaskar() {
        return kolaskar;
    }

    /**
     * @param kolaskar the kolaskar to set
     */
    public void setKolaskar(double kolaskar) {
        this.kolaskar = kolaskar;
    }

    /**
     * @return the bepipred2
     */
    public double getBepipred2() {
        return bepipred2;
    }

    /**
     * @param bepipred2 the bepipred2 to set
     */
    public void setBepipred2(double bepipred2) {
        this.bepipred2 = bepipred2;
    }


    public AminoEpitopo(String amino, int posicao) {
        this.amino = amino;
        this.position = posicao;
    }
    

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    
    /**
     * @return the amino
     */
    public String getAmino() {
        return amino;
    }

    /**
     * @param amino the amino to set
     */
    public void setAmino(String amino) {
        this.amino = amino;
    }

}
