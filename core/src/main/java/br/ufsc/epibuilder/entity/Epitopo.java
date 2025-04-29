/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity;

import br.udesc.cav.tese.glycosylation.GlycosylationFinder;
import br.udesc.cav.tese.glycosylation.Motif;
import br.ufsc.epibuilder.entity.report.FormatHelper;
import br.ufsc.epibuilder.proteomics.ProteomicCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;

/**
 *
 * @author renato
 */
public class Epitopo {

    private String sequence = "";
    private int start = 0;
    private int end = 0;
    private ArrayList<AminoEpitopo> aaEpitopes = new ArrayList<>();
    private ArrayList<Motif> nGlycMotifs = new ArrayList<>();

    public ArrayList<AminoEpitopo> getAaEpitopes() {
        return aaEpitopes;
    }

    public void addAminoEpitopo(AminoEpitopo amino) {
        aaEpitopes.add(amino);
    }

    public boolean isNglycolised() {
        return GlycosylationFinder.isNGlycMotif(sequence);
    }

    public void processNglycMotif() {
        nGlycMotifs = GlycosylationFinder.getNGlycMotif(sequence);
    }

    public ArrayList<Motif> getnGlycMotifs() {
        return nGlycMotifs;
    }

    public String getNglycMotifs() {
        if (nGlycMotifs.isEmpty()) {
            return "-";
        }
        String delim = "-";
        return FormatHelper.getListAsString(delim, nGlycMotifs);
    }

    public void append(String seq) {
        sequence += seq;
    }

    public double getMW() {
        double mw = 0;
        try {
            mw = ProteomicCalculator.getMolecularWeight(sequence);
        } catch (CompoundNotFoundException ex) {
            Logger.getLogger(Epitopo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mw;
    }

    public double getHydropathy() {
        double hp = 0;
        try {
            hp = ProteomicCalculator.getHydropathy(sequence);
        } catch (CompoundNotFoundException ex) {
            Logger.getLogger(Epitopo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hp;
    }

    public double getIP() {
        double ip = 0;
        try {
            ip = ProteomicCalculator.getIsoelectricPoint(sequence);
        } catch (CompoundNotFoundException ex) {
            Logger.getLogger(Epitopo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ip;
    }

    /**
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
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

    public double getPercent(SoftwareBcellEnum soft, double threshold) {
        double size = sequence.length();
        double total = 0;
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double val = aminoEpitopo.getValue(soft);
            if (val >= threshold) {
                total++;
            }
        }
        return total / size;
    }

    public double getPercentScore(double eminiThreshold, double parkerThreshold) {
        double size = sequence.length();
        double total = 0;
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double emini = aminoEpitopo.getValue(SoftwareBcellEnum.EMINI);
            double parker = aminoEpitopo.getValue(SoftwareBcellEnum.PARKER);

            if (emini >= eminiThreshold && parker >= parkerThreshold) {
                total++;
            }
        }
        return total / size;
    }

    /*
    public double getPercentScore(double eminiThreshold, double parkerThreshold, double karplusThreshold, double chouFosmanThreshold, double kolaskarThreshold) {
        double size = sequence.length();
        double total = 0;
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double emini = aminoEpitopo.getValue(SoftwareBcellEnum.EMINI);
            double parker = aminoEpitopo.getValue(SoftwareBcellEnum.PARKER);
            double karplus = aminoEpitopo.getValue(SoftwareBcellEnum.KARPLUS_SCHULZ);
            double chouFosman = aminoEpitopo.getValue(SoftwareBcellEnum.CHOU_FOSMAN);
            double kolaskar = aminoEpitopo.getValue(SoftwareBcellEnum.KOLASKAR);

            if (emini >= eminiThreshold && parker >= parkerThreshold && karplus >= karplusThreshold && chouFosman >= chouFosmanThreshold && kolaskar >= kolaskarThreshold) {

                total++;
            }
        }
        return total / size;
    }
     */
    public Topology getTopologyValue(SoftwareBcellEnum soft, double threshold) {
        String res = "";
        int total = 0;
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double val = aminoEpitopo.getValue(soft);
            if (val >= threshold) {
                res += "E";
                total++;
            } else {
                res += ".";
            }
        }
        double size = aaEpitopes.size();
        double val = total / size;

        return new Topology(res, val);
    }

    public Topology getNglycTopology() {
        String res = "";
        ArrayList<Integer> pos = new ArrayList<>();
        if (nGlycMotifs.isEmpty()) {
            res = sequence.replaceAll(".", ".");
        } else {
            
            for (int i = 0; i < sequence.length(); i++) {
                for (Motif nGlycMotif : nGlycMotifs) {
                    if (i >= nGlycMotif.getStart() - 1 && i <= nGlycMotif.getEnd() - 1) {
                        pos.add(i);
                    }
                }
            }
            char[] seq = sequence.replaceAll(".", ".").toCharArray();
            for (Integer i : pos) {
                seq[i] = 'E';
            }
            res = new String(seq);
        }

        return new Topology(res, ((double)pos.size())/(sequence.length()));
    }

    public Topology getHydropathyTopology() {
        String res = "";
        for (int i = 0; i < sequence.length(); i++) {
            String aa = sequence.charAt(i) + "";
            double hyd;
            try {
                hyd = ProteomicCalculator.getHydropathy(aa);
                if (hyd > 0) {
                    res += "+";
                } else if (hyd < 0) {
                    res += "-";
                } else {
                    res += ".";
                }
            } catch (CompoundNotFoundException ex) {
                Logger.getLogger(Epitopo.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return new Topology(res, 0);
    }

    public String getTopology(SoftwareBcellEnum soft, double threshold) {
        String res = "";
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double val = aminoEpitopo.getValue(soft);
            if (val >= threshold) {
                res += "E";
            } else {
                res += "-";
            }
        }
        return res;
    }

    public String getTopologyValidation(double eminiThreshold, double parkerThreshold, double karplusThreshold, double chouFosmanThreshold, double kolaskarThreshold) {
        String res = "";
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double emini = aminoEpitopo.getValue(SoftwareBcellEnum.EMINI);
            double parker = aminoEpitopo.getValue(SoftwareBcellEnum.PARKER);
            double karplus = aminoEpitopo.getValue(SoftwareBcellEnum.KARPLUS_SCHULZ);
            double chouFosman = aminoEpitopo.getValue(SoftwareBcellEnum.CHOU_FOSMAN);
            double kolaskar = aminoEpitopo.getValue(SoftwareBcellEnum.KOLASKAR);

            if (emini >= eminiThreshold && parker >= parkerThreshold && karplus >= karplusThreshold && chouFosman >= chouFosmanThreshold && kolaskar >= kolaskarThreshold) {
                res += "E";
            } else {
                res += ".";
            }
        }
        return res;
    }

    public String getTopologyValidation(double eminiThreshold, double parkerThreshold) {
        String res = "";
        for (AminoEpitopo aminoEpitopo : aaEpitopes) {
            double emini = aminoEpitopo.getValue(SoftwareBcellEnum.EMINI);
            double parker = aminoEpitopo.getValue(SoftwareBcellEnum.PARKER);
            if (emini >= eminiThreshold && parker >= parkerThreshold) {
                res += "E";
            } else {
                res += ".";
            }
        }
        return res;
    }

    public float getBestSequenceAminoSizeScore(double eminiThreshold, double parkerThreshold, double karplusThreshold, double chouFosmanThreshold, double kolaskarThreshold) {
        int i = 0;
        TreeSet<Integer> areas = new TreeSet<>();
        for (AminoEpitopo ae : getAaEpitopes()) {
            if (ae.getEmini() >= eminiThreshold
                    && ae.getParker() >= parkerThreshold
                    && ae.getKarplus_schulz() >= karplusThreshold
                    && ae.getChou_fasman() >= chouFosmanThreshold
                    && ae.getKolaskar() >= kolaskarThreshold) {
                i++;
            } else {
                if (i > 0) {
                    areas.add(i);
                }
                i = 0;
            }

        }
        if (!areas.isEmpty()) {
            return ((float) areas.last()) / (getSequence().length());

        } else {
            return 0;
        }
    }

    public float getBestSequenceAminoSizeScore(double mediaEmini, double mediaParker) {
        int i = 0;
        TreeSet<Integer> areas = new TreeSet<>();
        for (AminoEpitopo ae : getAaEpitopes()) {
            if (ae.getEmini() >= mediaEmini && ae.getParker() >= mediaParker) {
                i++;
            } else {
                if (i > 0) {
                    areas.add(i);
                }
                i = 0;
            }

        }
        if (!areas.isEmpty()) {
            return ((float) areas.last()) / (getSequence().length());

        } else {
            return 0;
        }
    }

    public String getTopologyBestSequenceAmino(double mediaEmini, double mediaParker) {
        String pep = "";
        ArrayList<String> areas = new ArrayList<>();
        for (AminoEpitopo ae : getAaEpitopes()) {
            if (ae.getEmini() >= mediaEmini && ae.getParker() >= mediaParker) {
                pep += ae.getAmino();
            } else {
                if (pep.length() > 0) {
                    areas.add(pep);
                }
                pep = "";
            }
        }
        areas.sort(new Comparator<String>() {
            @Override
            public int compare(String arg0, String arg1) {
                return Integer.compare(arg1.length(), arg0.length());
            }
        });

        String choose = "";
        if (!areas.isEmpty()) {
            choose = areas.get(0);
        }
        return getStline(getSequence(), choose);
    }

    public String getTopologyBestSequenceAmino(double eminiThreshold, double parkerThreshold, double karplusThreshold, double chouFosmanThreshold, double kolaskarThreshold) {
        String pep = "";
        ArrayList<String> areas = new ArrayList<>();
        for (AminoEpitopo ae : getAaEpitopes()) {
            if (ae.getEmini() >= eminiThreshold
                    && ae.getParker() >= parkerThreshold
                    && ae.getKarplus_schulz() >= karplusThreshold
                    && ae.getChou_fasman() >= chouFosmanThreshold
                    && ae.getKolaskar() >= kolaskarThreshold) {
                pep += ae.getAmino();
            } else {
                if (pep.length() > 0) {
                    areas.add(pep);
                }
                pep = "";
            }
        }
        areas.sort(new Comparator<String>() {
            @Override
            public int compare(String arg0, String arg1) {
                return Integer.compare(arg1.length(), arg0.length());
            }
        });

        String choose = "";
        if (!areas.isEmpty()) {
            choose = areas.get(0);
        }
        return getStline(getSequence(), choose);
    }

    public String getStline(String str, String substr) {
        int start = str.indexOf(substr);
        int end = start + substr.length() - 1;

        String c = "";
        for (int i = 0; i < str.length(); i++) {
            if (start == -1) {
                c += ".";
            } else if (i >= start && i <= end) {
                c += "E";
            } else {
                c += ".";
            }

        }
        return c;
    }
}
