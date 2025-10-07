/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity.report;

import br.ufsc.epibuilder.entity.SoftwareBcellEnum;
import br.ufsc.epibuilder.entity.Topology;

/**
 *
 * @author renato
 */
public class EpitopeReport {

    private double threshold;
    private SoftwareBcellEnum method;
    private Topology topology;
    private double avgScore;
    
    

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public EpitopeReport(double threshold, SoftwareBcellEnum method, Topology topology, double avgScore) {
        this.threshold = threshold;
        this.method = method;
        this.topology = topology;
        this.avgScore = avgScore;
    }

    public double getThreshold() {
        return threshold;
    }

    public SoftwareBcellEnum getMethod() {
        return method;
    }

    public Topology getTopology() {
        return topology;
    }

}
