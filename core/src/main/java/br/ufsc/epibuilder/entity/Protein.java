package br.ufsc.epibuilder.entity;

import br.udesc.cav.tese.glycosylation.Motif;
import br.ufsc.epibuilder.Parameters;
import br.ufsc.epibuilder.entity.report.FormatHelper;
import java.util.ArrayList;

public class Protein implements Comparable<Protein> {

    private String id;
    private String sequence;
    private ArrayList<AminoEpitopo> aminoEpitopos = new ArrayList<>();
    private boolean nglyco = false;
    private ArrayList<Motif> nglycMotifs = new ArrayList<>();
    private ArrayList<Epitopo> epitopes = new ArrayList<>();
    private String description;

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    
    public String getNglycMotifsAsString(){
        return FormatHelper.getListAsString("-", nglycMotifs);
    }

    public ArrayList<Motif> getNglycMotifs() {
        return nglycMotifs;
    }

    public void setNglycMotifs(ArrayList<Motif> nglycMotifs) {
        this.nglycMotifs = nglycMotifs;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
    
    public String getSequence(){
        String res = "";
        for (AminoEpitopo aminoEpitopo : aminoEpitopos) {
            res+=aminoEpitopo.getAmino();
        }
        return res;
    }

    public ArrayList<Epitopo> getEpitopes() {
        return epitopes;
    }

    public void setNglyco(boolean nglyco) {
        this.nglyco = nglyco;
    }

    public boolean isNglyco() {
        return nglyco;
    }

    public void process(double corte, int qtdInicial, int qtdFinal) {

        Epitopo epitopo = new Epitopo();
        ArrayList<Epitopo> epitopos = new ArrayList<>();

        for (AminoEpitopo aminoEpitopo : aminoEpitopos) {
            if (aminoEpitopo.getBepipred2() >= corte) {
                if (epitopo.getSequence().isEmpty()) {
                    epitopo.setStart(aminoEpitopo.getPosition());
                }
                epitopo.append(aminoEpitopo.getAmino());
                epitopo.addAminoEpitopo(aminoEpitopo);
            } else {
                if (!epitopo.getSequence().isEmpty()) {
                    epitopo.setEnd(aminoEpitopo.getPosition());
                    epitopos.add(epitopo);
                }
                epitopo = new Epitopo();
            }
        }
        if (!epitopo.getSequence().isEmpty()) {
            epitopo.setEnd(epitopo.getSequence().length());
            epitopos.add(epitopo);
        }

        ArrayList<Epitopo> epitoposFinais = new ArrayList<>();

        for (Epitopo epitopo1 : epitopos) {
            if (epitopo1.getSequence().length() >= qtdInicial && epitopo1.getSequence().length() <= qtdFinal) {
                epitoposFinais.add(epitopo1);
                if (epitopo1.isNglycolised()) {
                    setNglyco(true);
                }
            }
        }

        epitopes.clear();
        epitopes.addAll(epitoposFinais);
        //Process Nglyc motif
        for (Epitopo ept : epitoposFinais) {
            ept.processNglycMotif();
        }
    }

    public void addAminoEpitopo(String amino, int posicao, double bepipred2) {
        AminoEpitopo a = new AminoEpitopo(amino, posicao);
        a.setBepipred2(bepipred2);

        addAminoEpitopo(a);
    }

    public void addAminoEpitopo(AminoEpitopo aminoEpitopo) {
        aminoEpitopos.add(aminoEpitopo);
    }

    public ArrayList<AminoEpitopo> getAminoEpitopos() {
        return aminoEpitopos;
    }

    public void setAminoEpitopos(ArrayList<AminoEpitopo> aminoEpitopos) {
        this.aminoEpitopos = aminoEpitopos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int compareTo(Protein o) {
        return id.compareTo(o.id);
    }

    public double getThreshold(SoftwareBcellEnum software) {
        if(Parameters.MAP_SOFTWARES.containsKey(software)){
            if(Parameters.MAP_SOFTWARES.get(software)!=null){
                return Parameters.MAP_SOFTWARES.get(software);
            }
        }
        int start = 3;
        int end = 3;
        
        double threshold = 0;
        if (software == SoftwareBcellEnum.EMINI) {
            start = 2;
        }
        if(software==SoftwareBcellEnum.KARPLUS_SCHULZ){
            end = 4;
        }
        for (int i = start; i < aminoEpitopos.size()-end; i++) {
            
            AminoEpitopo aa = aminoEpitopos.get(i);
            switch (software) {
                case PARKER: {
                    threshold += aa.getParker();
                    break;
                }
                case EMINI: {
                    threshold += aa.getEmini();
                    break;
                }
                case KOLASKAR: {
                    threshold += aa.getKolaskar();
                    break;
                }
                case CHOU_FOSMAN: {
                    threshold += aa.getChou_fasman();
                    break;
                }
                case KARPLUS_SCHULZ: {
                    threshold += aa.getKarplus_schulz();
                    break;
                }
            }
        }
        return threshold / (aminoEpitopos.size()-(end+start));
    }
}
