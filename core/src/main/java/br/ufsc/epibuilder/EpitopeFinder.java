package br.ufsc.epibuilder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import br.udesc.cav.tese.glycosylation.Motif;
import br.udesc.epibuilder.blast.Blast;
import static br.udesc.epibuilder.blast.BlastRunner.getBlastResults;
import br.udesc.epibuilder.blast.ReportBlastJoiner;
import br.ufsc.epibuilder.entity.Proteome;
import br.ufsc.epibuilder.converter.IEDBBcellCalculator;
import br.ufsc.epibuilder.converter.ProteinConverter;

import br.ufsc.epibuilder.entity.AminoEpitopo;
import br.ufsc.epibuilder.entity.Epitopo;
import br.ufsc.epibuilder.entity.Protein;
import br.ufsc.epibuilder.entity.ReportBCell;
import br.ufsc.epibuilder.entity.SoftwareBcellEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static br.ufsc.epibuilder.Parameters.*;
import br.ufsc.epibuilder.converter.BepiPred3Converter;
import static br.ufsc.epibuilder.entity.SoftwareBcellEnum.CHOU_FOSMAN;
import static br.ufsc.epibuilder.entity.SoftwareBcellEnum.EMINI;
import static br.ufsc.epibuilder.entity.SoftwareBcellEnum.KARPLUS_SCHULZ;
import static br.ufsc.epibuilder.entity.SoftwareBcellEnum.KOLASKAR;
import static br.ufsc.epibuilder.entity.SoftwareBcellEnum.PARKER;
import static br.ufsc.epibuilder.entity.report.FormatHelper.*;
import br.ufsc.epibuilder.entity.Topology;
import br.ufsc.epibuilder.entity.report.EpitopeReport;
import br.ufsc.epibuilder.entity.report.ExcelReport;
import br.ufsc.epibuilder.entity.report.ExcelTabReport;
import br.ufsc.epibuilder.entity.report.FormatHelper;
import br.ufsc.epibuilder.entity.report.Report;
import br.ufsc.epibuilder.proteomics.ProteomicCalculator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;

/**
 *
 * @author renato
 */
public class EpitopeFinder {

    public static EpitopeCount count(String epitopo, Proteome proteome) {
        int total = 0;
        int totalProteins = 0;
        StringBuilder ids = new StringBuilder();
        for (ProteinConverter protein : proteome.getProteins()) {
            int count = protein.count(epitopo);
            total += count;
            if (count > 0) {
                totalProteins++;
                if (Parameters.HIT_ACCESSION) {
                    ids.append(protein.getId());
                    ids.append(" ");
                }
            }
        }
        if (!Parameters.HIT_ACCESSION) {
            return new EpitopeCount("-", total, totalProteins);
        }
        return new EpitopeCount(ids.toString().trim(), total, totalProteins);
    }

    public static int countByOrganism(String epitopo, Proteome proteome) {
        int total = 0;
        for (ProteinConverter protein : proteome.getProteins()) {
            total += protein.count(epitopo);
        }
        return total;
    }

    public static int countByOrganism(Epitopo epitopo, Proteome proteome) {
        int total = 0;
        for (ProteinConverter protein : proteome.getProteins()) {
            total += protein.count(epitopo.getSequence());
        }
        return total;
    }

    public static String concatIdByOrganism(Epitopo epitopo, Proteome proteome) {
        String total = "";
        for (ProteinConverter protein : proteome.getProteins()) {
            int count = protein.count(epitopo.getSequence());
            if (count > 0) {
                total += protein.getId() + " ";
            }
        }
        return total.trim();
    }

    public static String concatIdByOrganism(String epitopo, Proteome proteome) {
        ArrayList<String> ids = new ArrayList<>();
        for (ProteinConverter protein : proteome.getProteins()) {
            int count = protein.count(epitopo);
            if (count > 0) {
                ids.add(protein.getId());
            }
        }
        if (ids.isEmpty()) {
            return "-";
        }
        return FormatHelper.getListAsString(",", ids);
    }

    public static HashMap<String, Integer> getMapOrganismoCount(Epitopo epitopo, ArrayList<Proteome> proteomes) {
        HashMap<String, Integer> resultado = new HashMap<>();

        for (Proteome proteome : proteomes) {
            int total = 0;
            for (ProteinConverter protein : proteome.getProteins()) {
                total += protein.count(epitopo.getSequence());
            }
            resultado.put(proteome.getOrganism(), total);
        }

        return resultado;
    }

    public static boolean validateEpitope(Epitopo epitopo, double mediaEmini, double mediaParker, int qtd) {
        int i = 0;
        TreeSet<Integer> areas = new TreeSet<>();
        for (AminoEpitopo ae : epitopo.getAaEpitopes()) {
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
            if (areas.last() >= qtd) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static Map<String, ProteinConverter> getMap(ArrayList<ProteinConverter> proteins) {
        HashMap<String, ProteinConverter> map = new HashMap<>();
        for (ProteinConverter protein : proteins) {
            map.put(protein.getId(), protein);
        }
        return map;
    }

    public static int getTotalAminoacids(ArrayList<ProteinConverter> proteins) {
        int total = 0;
        for (ProteinConverter protein : proteins) {
            total += protein.getAminoacidMap().size();
        }
        return total;
    }

    public static String process() {
        try {
            setOutput();

            sout("Process started: ");
            ArrayList<ProteinConverter> proteins = new ArrayList<>();

            sout("Reading BepiPred-3.0 file");
            if(Parameters.BEPIPRED_INPUT == BEPIPRED_TYPE.FASTA){
                sout("Source file: FASTA");
                sout("Executing BepiPred-3.0");
                BepiPred3Runner.execute();
                sout("BepiPred-3.0 finished");
            }
            sout("Loading BepiPred-3.0 - CSV file");
            proteins = BepiPred3Converter.getBepipred3FromBiolib(BEPIPRED_FILE);
            sout("BepiPred-3.0 - Done");

            Map<String, ProteinConverter> bepipredMap = getMap(proteins);

            HashMap<String, Protein> map = new HashMap<>();
            sout("Converting AA in Protein");
            for (String id : bepipredMap.keySet()) {
                ProteinConverter protein = bepipredMap.get(id);

                for (Integer pos : protein.getAminoacidMap().keySet()) {
                    ProteinConverter.Amino aminoBepipred2 = protein.getAminoacidMap().get(pos);

                    String amino = aminoBepipred2.getAa();
                    double bepipred2 = aminoBepipred2.getValue();

                    AminoEpitopo aa = new AminoEpitopo(amino, pos);

                    aa.setBepipred2(bepipred2);

                    Protein pe = map.get(id);
                    if (pe == null) {
                        pe = new Protein();
                        pe.setId(id);
                        pe.addAminoEpitopo(aa);
                        map.put(id, pe);
                    } else {
                        pe.addAminoEpitopo(aa);
                    }
                }

            }
            sout("Converting AA in Protein - Done");

            sout("Processing Methods");
            ArrayList<Protein> proteinList = new ArrayList<>(map.values());
            for (Protein re : proteinList) {
                String sequence = re.getSequence();
                ArrayList<ReportBCell> list_chou_fosman = IEDBBcellCalculator.run(CHOU_FOSMAN, sequence);
                ArrayList<ReportBCell> list_emini = IEDBBcellCalculator.run(EMINI, sequence);
                ArrayList<ReportBCell> list_karplus = IEDBBcellCalculator.run(KARPLUS_SCHULZ, sequence);
                ArrayList<ReportBCell> list_kolaskar = IEDBBcellCalculator.run(KOLASKAR, sequence);
                ArrayList<ReportBCell> list_parker = IEDBBcellCalculator.run(PARKER, sequence);

                for (AminoEpitopo aa : re.getAminoEpitopos()) {
                    int pos = aa.getPosition();
                    aa.setParker(IEDBBcellCalculator.getScoreByPosition(pos + 1, list_parker));
                    aa.setEmini(IEDBBcellCalculator.getScoreByPosition(pos + 1, list_emini));
                    aa.setKarplus_schulz(IEDBBcellCalculator.getScoreByPosition(pos + 1, list_karplus));
                    aa.setKolaskar(IEDBBcellCalculator.getScoreByPosition(pos + 1, list_kolaskar));
                    aa.setChou_fasman(IEDBBcellCalculator.getScoreByPosition(pos + 1, list_chou_fosman));
                }
            }
            sout("Processing Methods - Done");

            int totalNglyc = 0;
            int totalEpitopes = 0;
            int totalEpitopesNglyc = 0;

            sout("Building epitopes");
            for (Protein re : proteinList) {
                re.process(THRESHOLD_BEPIPRED, MIN_LENGTH_BEPIPRED, MAX_LENGTH_BEPIPRED);
                for (Epitopo epitopo : re.getEpitopes()) {
                    totalEpitopes++;
                    if (epitopo.isNglycolised()) {
                        totalEpitopesNglyc++;
                    }
                }
                if (re.isNglyco()) {
                    totalNglyc++;
                }
            }
            sout("Building epitopes - Done");

            sout("Creating report's structure");
            ArrayList<Report> reportList = new ArrayList<>();
            for (Protein re : proteinList) {
                ArrayList<Epitopo> epitopos = re.getEpitopes();

                for (Epitopo epitopo : epitopos) {
                    Report rep = new Report();
                    rep.setProteinId(re.getId());
                    rep.setStart(epitopo.getStart());
                    rep.setEnd(epitopo.getEnd());
                    rep.setnGlyc(epitopo.isNglycolised() ? "Y" : "N");
                    rep.setLength(epitopo.getSequence().length());
                    rep.setEpitope(epitopo.getSequence());
                    rep.setMw(epitopo.getMW());
                    rep.setIp(epitopo.getIP());
                    rep.setAvgHydropathy(epitopo.getHydropathy());
                    rep.setNglycmotif(epitopo.getNglycMotifs());
                    rep.setnGlycMotifs(epitopo.getnGlycMotifs());
                    rep.setnGlycTopology(epitopo.getNglycTopology());
                    rep.setHydropathyTopology(epitopo.getHydropathyTopology());

                    int total = 0;
                    double sumCover = 0;
                    for (SoftwareBcellEnum softwareBcellEnum : Parameters.MAP_SOFTWARES.keySet()) {
                        double threshold = re.getThreshold(softwareBcellEnum);
                        Topology topology = epitopo.getTopologyValue(softwareBcellEnum, threshold);
                        total++;
                        sumCover += topology.getValue();

                        double totalScore = 0;

                        for (AminoEpitopo aminoEpitopo : epitopo.getAaEpitopes()) {
                            switch (softwareBcellEnum) {
                                case PARKER:
                                    totalScore += aminoEpitopo.getParker();
                                    break;
                                case CHOU_FOSMAN:
                                    totalScore += aminoEpitopo.getChou_fasman();
                                    break;
                                case EMINI:
                                    totalScore += aminoEpitopo.getEmini();
                                    break;
                                case KARPLUS_SCHULZ:
                                    totalScore += aminoEpitopo.getKarplus_schulz();
                                    break;
                                case KOLASKAR:
                                    totalScore += aminoEpitopo.getKolaskar();
                                    break;
                            }
                        }
                        double avgScore = totalScore / epitopo.getAaEpitopes().size();
                        rep.addEpitopeReport(new EpitopeReport(threshold, softwareBcellEnum, topology, avgScore));
                    }
                    double avgCover = sumCover / total;
                    rep.setAvgCover(avgCover);

                    double totalBepipred = 0;
                    for (AminoEpitopo aminoEpitopo : epitopo.getAaEpitopes()) {
                        totalBepipred += aminoEpitopo.getBepipred2();
                    }
                    double avgBepiPred = totalBepipred / epitopo.getAaEpitopes().size();

                    rep.setAvgBepipredScore(avgBepiPred);

                    reportList.add(rep);

                }
            }

            Collections.sort(reportList, new Comparator<Report>() {
                @Override
                public int compare(Report arg0, Report arg1) {
                    return arg1.getAvgCover().compareTo(arg0.getAvgCover());
                }
            });
            sout("Creating report's structure - Done");

            sout("Creating reports");
            String dest = Parameters.DESTINATION_FOLDER;
            String basename = Parameters.BASENAME;
            if (basename.trim().length() > 0) {
                basename += "-";
            }

            sout("\t Parameters\t");
            StringBuilder stParameters = new StringBuilder();
            stParameters.append("\n---- Running Parameters ----");
            stParameters.append("\nBepiPred-3.0 Threshold : " + THRESHOLD_BEPIPRED);
            stParameters.append("\nMin epitope length     : " + MIN_LENGTH_BEPIPRED);
            stParameters.append("\nMax epitope length     : " + MAX_LENGTH_BEPIPRED);
            stParameters.append("\n");
            stParameters.append("\n---- Softwares Threshold ----");
            if (Parameters.MAP_SOFTWARES.containsKey(EMINI)) {
                stParameters.append("\nEmini                  : " + (Parameters.MAP_SOFTWARES.get(EMINI) == null ? "Default" : Parameters.MAP_SOFTWARES.get(EMINI)));
            }
            if (Parameters.MAP_SOFTWARES.containsKey(PARKER)) {
                stParameters.append("\nParker                 : " + (Parameters.MAP_SOFTWARES.get(PARKER) == null ? "Default" : Parameters.MAP_SOFTWARES.get(PARKER)));
            }
            if (Parameters.MAP_SOFTWARES.containsKey(CHOU_FOSMAN)) {
                stParameters.append("\nChou Fosman            : " + (Parameters.MAP_SOFTWARES.get(CHOU_FOSMAN) == null ? "Default" : Parameters.MAP_SOFTWARES.get(CHOU_FOSMAN)));
            }
            if (Parameters.MAP_SOFTWARES.containsKey(KARPLUS_SCHULZ)) {
                stParameters.append("\nKarplus Schulz         : " + (Parameters.MAP_SOFTWARES.get(KARPLUS_SCHULZ) == null ? "Default" : Parameters.MAP_SOFTWARES.get(KARPLUS_SCHULZ)));
            }
            if (Parameters.MAP_SOFTWARES.containsKey(KOLASKAR)) {
                stParameters.append("\nKolaskar               : " + (Parameters.MAP_SOFTWARES.get(KOLASKAR) == null ? "Default" : Parameters.MAP_SOFTWARES.get(KOLASKAR)));
            }

            stParameters.append("\n\n---- Proteomes ----");
            stParameters.append("\n" + StringUtils.rightPad("Alias", 15, ' ') + "\tFile");
            for (Proteome proteome : Parameters.PROTEOMES) {
                stParameters.append("\n" + StringUtils.rightPad(proteome.getOrganism(), 15, ' ') + "\t" + proteome.getFile().getAbsolutePath());
            }

            if (Parameters.SEARCH_BLAST) {
                stParameters.append("\n\n---- Blast ----");
                stParameters.append("\nIdentity         : " + Parameters.BLAST_IDENTITY);
                stParameters.append("\nCover            : " + Parameters.BLAST_COVER);
                stParameters.append("\nWord size        : " + Parameters.BLAST_WORD_SIZE);
                stParameters.append("\nTask blastp-short: " + Parameters.BLAST_TASK);

            }
            stParameters.append("\n\n---- Stats ----");
            stParameters.append("\nProcessed proteins     : " + proteinList.size());
            stParameters.append("\nN-Glycosylated proteins : " + totalNglyc);
            stParameters.append("\nIdentified epitopes    : " + totalEpitopes);
            stParameters.append("\nN-Glycosylated epitopes: " + totalEpitopesNglyc);
            stParameters.append("\n");
            String fileParameters = saveRandomFileName(dest + "/" + basename + "epibuilder-parameters", stParameters.toString(), "txt");
            sout("\t Parameters - done\t");

            StringBuilder stReport = new StringBuilder();

            sout("\t Report by Protein\t");
            String reportByProtein = generateReportByProtein(proteinList);
            String fileEpibuilderProteinSummary = saveRandomFileName(dest + "/" + basename + "epibuilder-protein-summary", reportByProtein, "tsv");
            sout("\t Report by Protein - done\t");

            sout("\t Report by Topology\t");
            String reportTopology = generateReportByTopology(reportList);
            String fileEpibuilderTopology = saveRandomFileName(dest + "/" + basename + "epibuilder-topology", reportTopology, "tsv");

            sout("\t Report by Topology - Done\t");

            sout("\t Report Scores\t");
            String reportScores = generateMethodScore(proteinList);
            String fileEpibuilderScores = saveRandomFileName(dest + "/" + basename + "epibuilder-scores", reportScores, "tsv");

            sout("\t Report Scores - done\t");

            sout("\t Epitopes FASTA\t");

            String fileEpibuilderFastaEpitopo = saveRandomFileName(dest + "/" + basename + "epibuilder-epitopes-fasta", generateReportFastaEpitope(reportList), "fasta");
            sout("\t Epitopes FASTA - done\t");

            sout("\t Report Detailed\t");
            String reportDetailed = generateReportDetailed(reportList);

            //Performe a BLAST search and append it to the report
            if (Parameters.SEARCH_BLAST) {
                sout("\tBlast\t");
                //Each blast result append in the last reportDetaild textFile until generate the last
                for (Proteome proteome : Parameters.PROTEOMES) {
                    File blastoutput = getBlastResults(proteome, fileEpibuilderFastaEpitopo);
                    Blast blast = new Blast(proteome.getOrganism(), blastoutput);
                    reportDetailed = ReportBlastJoiner.joinReport(reportDetailed, blast.getListReport(Parameters.BLAST_IDENTITY, Parameters.BLAST_COVER), blast.getName());
                }
                sout("\tBlast - done\t");
            }
            String fileEpibuilderDetail = saveRandomFileName(dest + "/" + basename + "epibuilder-epitope-detail", reportDetailed, "tsv");
            sout("\t Report Detailed - Done\t");
            //END BLAST

            stReport.append(stParameters.toString());
            stReport.append("\n---- Protein Summary ----\n");
            stReport.append(convertTabString(reportByProtein));
            stReport.append("\n---- Detailed  ----\n");
            stReport.append(convertTabString(reportDetailed));
            stReport.append("\n---- Epitopes topology  ----\n");
            stReport.append(convertTabString(reportTopology));

            ArrayList<ExcelTabReport> excelTab = new ArrayList<>();

            excelTab.add(new ExcelTabReport("Epitopes Detailed", reportDetailed));
            excelTab.add(new ExcelTabReport("Protein Summary", reportByProtein));
            excelTab.add(new ExcelTabReport("Epitopes Topology", reportTopology));
            //excelTab.add(new ExcelTabReport("Scores", reportScores));

            boolean excelReport = false;
            String fileEpibuilderExcel = dest + "/" + basename + "epibuilder.xlsx";
            try {
                ExcelReport.generateExcelXlsx(excelTab, fileEpibuilderExcel);
                excelReport = true;
            } catch (Exception e) {
                sout("Error generating excel files:" + e.getMessage());
                e.printStackTrace();
            }

            stReport.append("Generated files:");
            stReport.append("\n\t");
            stReport.append(fileParameters);
            stReport.append("\n\t");
            stReport.append(fileEpibuilderProteinSummary);
            stReport.append("\n\t");
            stReport.append(fileEpibuilderTopology);
            stReport.append("\n\t");
            stReport.append(fileEpibuilderDetail);
            if (excelReport) {
                stReport.append("\n\t");
                stReport.append(fileEpibuilderExcel);
            }
            stReport.append("\n\t");
            stReport.append(fileEpibuilderScores);
            stReport.append("\n\t");
            stReport.append(fileEpibuilderFastaEpitopo);

            saveRandomFileName(dest + "/" + basename + "epibuilder", stReport.toString(), "txt");
            sout("Finish\t");
            return stReport.toString();
        } catch (Exception e) {
            sout("An error occured: "+ e.getMessage());
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static String generateReportByProtein(ArrayList<Protein> proteinList) {
        StringBuilder stParameters = new StringBuilder();
        stParameters.append("Id\tEpitopes\tN-Glyc\n");
        for (Protein re : proteinList) {
            int nglycLocal = 0;

            for (Epitopo epitopo : re.getEpitopes()) {
                if (epitopo.isNglycolised()) {
                    nglycLocal++;
                }
            }
            stParameters.append(String.format("%s\t%s\t%s\n", re.getId(), re.getEpitopes().size(), nglycLocal));
        }
        return stParameters.toString();
    }

    public static String saveRandomFileName(String name, String content, String ext) throws IOException {

        String file = name + "." + ext;

        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
        return file;

    }

    public static String generateEpitopeList(TreeSet<Report> reportList) {
        StringBuilder sb = new StringBuilder();
        sb.append("Protein Id\tEpitopes\tStart\tEnd\tN-glyc\tN-Glyc motifs");
        sb.append("\n");
        for (Report report : reportList) {
            sb.append(String.format("%s\t%s\t%s\t%s\t%s\t%s\n", report.getProteinId(), report.getEpitope(), report.getStart(), report.getEnd(), report.getnGlyc(), report.getNglycmotif()));
        }
        return sb.toString();
    }

    public static String generateReportFastaEpitope(ArrayList<Report> reportList) {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (Report report : reportList) {
            sb.append(String.format(">%s-%s\n%s\n", count++, report.getEpitope(), report.getEpitope()));
        }
        return sb.toString();
    }

    public static String generateReportByTopology(ArrayList<Report> reportList) {
        StringBuilder sb = new StringBuilder();
        sb.append("N\tId\t" + StringUtils.leftPad("Method", 15, ' ') + "\tThreshold\tAvg Score\tCover\tEpitope\tStart\tEnd\tN-Glyc\tN-Glyc-Count\tN-Glyc-Motifs\tLength\tkDa\tI.P\tAvg Hydropathy\tAvg Cover");
        sb.append("\n");
        int count = 1;
        for (Report report : reportList) {
            sb.append(String.format("%s\t%s\t%s\t%.2f\t%.2f\t-\t%s",
                    count++,
                    report.getProteinId(),
                    StringUtils.leftPad(SoftwareBcellEnum.BEPIPRED.description, 15, ' '),
                    Parameters.THRESHOLD_BEPIPRED,
                    report.getAvgBepipredScore(),
                    report.getEpitope()));

            sb.append(String.format("\t%s\t%s\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%.2f\t%.2f\n",
                    report.getStart(),
                    report.getEnd(),
                    report.getnGlyc(),
                    report.getnGlycMotifs().size(),
                    report.getNglycmotif(),
                    report.getLength(),
                    report.getMw() / 1000,
                    report.getIp(),
                    report.getAvgHydropathy(),
                    report.getAvgCover()
            ));

            for (EpitopeReport epitopeReport : report.getEpitopeReports()) {
                sb.append(String.format("\t\t%s\t%.2f\t%.2f\t%.2f\t%s%s\n",
                        StringUtils.leftPad(epitopeReport.getMethod().description, 15, ' '),
                        epitopeReport.getThreshold(),
                        epitopeReport.getAvgScore(),
                        epitopeReport.getTopology().getValue(),
                        epitopeReport.getTopology().getDescription(),
                        StringUtils.leftPad("\t", 10, ' ')));
            }
            if (!report.getEpitopeReports().isEmpty()) {
                sb.append(String.format("\t\t%s\t-\t-\t%.2f\t%s%s\n",
                        StringUtils.leftPad("All matches", 15, ' '),
                        report.getTopologyCoverValidation(),
                        report.getTopologyValidation(),
                        StringUtils.leftPad("\t", 10, ' ')));
            }
            sb.append(String.format("\t\t%s\t-\t-\t%.2f\t%s%s\n",
                    StringUtils.leftPad("N-Glyc", 15, ' '),
                    report.getnGlycTopology().getValue(),
                    report.getnGlycTopology().getDescription(),
                    StringUtils.leftPad("\t", 10, ' ')));
            sb.append(String.format("\t\t%s\t-\t%.2f\t-\t%s%s\n",
                    StringUtils.leftPad("Hydropathy", 15, ' '),
                    report.getAvgHydropathy(),
                    report.getHydropathyTopology().getDescription(),
                    StringUtils.leftPad("\t", 10, ' ')));
        }
        return sb.toString();
    }

    public static String getNglycTopology(Report report) {
        String res = "";
        String sequence = report.getEpitope();
        ArrayList<Motif> motifs = report.getnGlycMotifs();
        for (int i = 0; i < sequence.length(); i++) {

        }
        return res;
    }

    public static String generateReportDetailed(ArrayList<Report> reportList) {
        String stOrganismCount = "";

        for (Proteome proteome : Parameters.PROTEOMES) {
            try {
                sout("\t\tLoading proteome: " + proteome.getOrganism() + "\t" + proteome.getFile().getAbsolutePath() + "\t");
                proteome.load();
                sout("\t\tLoaded " + proteome.getProteins().size() + " proteins\t");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        for (Proteome proteome : Parameters.PROTEOMES) {
            stOrganismCount += "\t" + proteome.getOrganism() + "_count\t" + proteome.getOrganism() + "_acc";
        }

        /*        String stOrganismId = "";
        for (Proteome proteome : Parameters.PROTEOMES) {
            stOrganismId += "\t" + proteome.getOrganism() + "_acc";
        }*/
        String stMethod = "";
        for (SoftwareBcellEnum softwareBcellEnum : Parameters.MAP_SOFTWARES.keySet()) {
            stMethod += "\t" + softwareBcellEnum.description;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("N\tId\tEpitope\tStart\tEnd\tN-Glyc\tN-Glyc-Count\tN-Glyc-Motifs\tLength\tMW(kDa)\tI.P\tHydropathy\tAll Matches Cover\tAvg Cover\tBepiPred3" + stMethod + stOrganismCount + "\n");
        int count = 1;
        for (Report report : reportList) {
            String stOrganismEpitopeCount = "";
            String stMethodScore = "";

            //String stOrganismEpitopeProteinId = "";
            for (Proteome proteome : Parameters.PROTEOMES) {
                sout(String.format("Start searching for epitope: %s\t%s/%s in %s(%s proteins)", report.getEpitope(), count, reportList.size(), proteome.getOrganism(), proteome.getProteins().size()));
                //sout("Start searching for epitope: " + count + "/" + reportList.size() + " in " + proteome.getOrganism() + " - " + report.getEpitope());
                EpitopeCount epitopeCount = count(report.getEpitope(), proteome);

                stOrganismEpitopeCount += "\t" + epitopeCount.getTotalhits() + "\t" + epitopeCount.getIds();
                //stOrganismEpitopeProteinId += "\t" + epitopeCount.getIds();
                sout(String.format("End searching for epitope: %s\t%s/%s in %s(%s proteins). Found (%s hits)", report.getEpitope(), count, reportList.size(), proteome.getOrganism(), proteome.getProteins().size(), epitopeCount.getTotalhits()));
            }

            for (EpitopeReport epitopeReport : report.getEpitopeReports()) {
                stMethodScore += String.format("\t%.2f", epitopeReport.getAvgScore());
            }

            sb.append(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f%s%s\n",
                    count++,
                    report.getProteinId(),
                    report.getEpitope(),
                    report.getStart(),
                    report.getEnd(),
                    report.getnGlyc(),
                    report.getnGlycMotifs().size(),
                    report.getNglycmotif(),
                    report.getLength(),
                    report.getMw() / 1000,
                    report.getIp(),
                    report.getAvgHydropathy(),
                    report.getTopologyCoverValidation(),
                    report.getAvgCover(),
                    report.getAvgBepipredScore(),
                    stMethodScore,
                    stOrganismEpitopeCount
            //stOrganismEpitopeProteinId
            ));
        }

        System.gc();
        return sb.toString();
    }

    public static void sout(String st) {
        System.out.println(Calendar.getInstance().getTime() + "\t" + st);
    }

    public static String generateMethodScore(ArrayList<Protein> proteinas) throws CompoundNotFoundException {
        String stMethod = "";
        for (SoftwareBcellEnum softwareBcellEnum : Parameters.MAP_SOFTWARES.keySet()) {
            stMethod += "\t" + softwareBcellEnum.description;
        }
        StringBuffer sb = new StringBuffer("Id\tPosition\tResidue\tBepipred3" + stMethod + "\tMW\tIP\tHydropathy\n");
        for (Protein proteina : proteinas) {
            for (AminoEpitopo aminoEpitopo : proteina.getAminoEpitopos()) {

                stMethod = "";
                for (SoftwareBcellEnum softwareBcellEnum : Parameters.MAP_SOFTWARES.keySet()) {
                    switch (softwareBcellEnum) {
                        case PARKER:
                            stMethod += String.format("\t%.2f", aminoEpitopo.getParker());
                            break;
                        case CHOU_FOSMAN:
                            stMethod += String.format("\t%.2f", aminoEpitopo.getChou_fasman());
                            break;
                        case EMINI:
                            stMethod += String.format("\t%.2f", aminoEpitopo.getEmini());
                            break;
                        case KARPLUS_SCHULZ:
                            stMethod += String.format("\t%.2f", aminoEpitopo.getKarplus_schulz());
                            break;
                        case KOLASKAR:
                            stMethod += String.format("\t%.2f", aminoEpitopo.getKolaskar());
                            break;
                    }
                }
                String res = String.format("%s\t%s\t%s\t%.2f%s\t%.2f\t%.2f\t%.2f\n",
                        proteina.getId(),
                        aminoEpitopo.getPosition() + 1,
                        aminoEpitopo.getAmino(),
                        aminoEpitopo.getBepipred2(),
                        stMethod,
                        ProteomicCalculator.getMolecularWeight(aminoEpitopo.getAmino()),
                        ProteomicCalculator.getIsoelectricPoint(aminoEpitopo.getAmino()),
                        ProteomicCalculator.getHydropathy(aminoEpitopo.getAmino())
                );
                sb.append(res);

            }
        }
        return sb.toString();
    }

    private static void setOutput() throws Exception {
        if (Parameters.OUTPUT_FILE) {
            File file = new File(Parameters.DESTINATION_FOLDER + "/" + Parameters.BASENAME + "-epibuilder.log");
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        }
    }
}
