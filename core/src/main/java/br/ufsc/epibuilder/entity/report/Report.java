/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity.report;

import br.udesc.cav.tese.glycosylation.Motif;
import br.ufsc.epibuilder.entity.Topology;
import java.util.ArrayList;

/**
 *
 * @author renato
 */
public class Report {

    private String proteinId;
    private int start;
    private int end;
    private String nGlyc;
    private int length;
    private Double avgCover;
    private String epitope;
    private ArrayList<EpitopeReport> epitopeReports = new ArrayList<>();
    private Double avgBepipredScore;
    private double mw;
    private double ip;
    private String nglycmotif;
    private double avgHydropathy;
    private ArrayList<Motif> nGlycMotifs = new ArrayList<>();
    private Topology nGlycTopology;
    private Topology hydropathyTopology;
    private String proteinDescription;

    public void setProteinDescription(String proteinDescription) {
        this.proteinDescription = proteinDescription;
    }

    public String getProteinDescription() {
        return proteinDescription;
    }

    public void setHydropathyTopology(Topology hydropathyTopology) {
        this.hydropathyTopology = hydropathyTopology;
    }

    public Topology getHydropathyTopology() {
        return hydropathyTopology;
    }

    public void setnGlycTopology(Topology nGlycTopology) {
        this.nGlycTopology = nGlycTopology;
    }

    public Topology getnGlycTopology() {
        return nGlycTopology;
    }

    public void setnGlycMotifs(ArrayList<Motif> nGlycMotifs) {
        this.nGlycMotifs = nGlycMotifs;
    }

    public ArrayList<Motif> getnGlycMotifs() {
        return nGlycMotifs;
    }

    public void setAvgHydropathy(double avgHydropathy) {
        this.avgHydropathy = avgHydropathy;
    }

    public double getAvgHydropathy() {
        return avgHydropathy;
    }

    public void setNglycmotif(String nglycmotif) {
        this.nglycmotif = nglycmotif;
    }

    public String getNglycmotif() {
        return nglycmotif;
    }

    public double getIp() {
        return ip;
    }

    public double getMw() {
        return mw;
    }

    public void setIp(double ip) {
        this.ip = ip;
    }

    public void setMw(double mw) {
        this.mw = mw;
    }

    public void setAvgBepipredScore(Double avgBepipredScore) {
        this.avgBepipredScore = avgBepipredScore;
    }

    public Double getAvgBepipredScore() {
        return avgBepipredScore;
    }

    public void addEpitopeReport(EpitopeReport ept) {
        epitopeReports.add(ept);
    }

    /**
     * @return the proteinId
     */
    public String getProteinId() {
        return proteinId;
    }

    /**
     * @param proteinId the proteinId to set
     */
    public void setProteinId(String proteinId) {
        this.proteinId = proteinId;
    }

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public int getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * @return the nGlyc
     */
    public String getnGlyc() {
        return nGlyc;
    }

    /**
     * @param nGlyc the nGlyc to set
     */
    public void setnGlyc(String nGlyc) {
        this.nGlyc = nGlyc;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @return the avgCover
     */
    public Double getAvgCover() {
        return avgCover;
    }

    /**
     * @param avgCover the avgCover to set
     */
    public void setAvgCover(Double avgCover) {
        this.avgCover = avgCover;
    }

    /**
     * @return the epitope
     */
    public String getEpitope() {
        return epitope;
    }

    /**
     * @param epitope the epitope to set
     */
    public void setEpitope(String epitope) {
        this.epitope = epitope;
    }

    /**
     * @return the epitopeReports
     */
    public ArrayList<EpitopeReport> getEpitopeReports() {
        return epitopeReports;
    }

    /**
     * @param epitopeReports the epitopeReports to set
     */
    public void setEpitopeReports(ArrayList<EpitopeReport> epitopeReports) {
        this.epitopeReports = epitopeReports;
    }

    public String getTopologyValidation() {
        String res = "";
        String[] tops = new String[epitopeReports.size()];

        for (int i = 0; i < epitopeReports.size(); i++) {
            tops[i] = epitopeReports.get(i).getTopology().getDescription();
        }

        for (int i = 0; i < epitope.length(); i++) {
            String aux = "E";
            for (int j = 0; j < tops.length; j++) {
                if (tops[j].charAt(i) != 'E') {
                    aux = ".";
                    break;
                }
            }
            res += aux;
        }

        return res;
    }

    public Double getTopologyCoverValidation() {
        String res = getTopologyValidation();
        double total = res.length();
        double e = 0;
        for (int i = 0; i < res.length(); i++) {
            if (res.charAt(i) == 'E') {
                e++;
            }
        }

        return e / total;
    }
}
