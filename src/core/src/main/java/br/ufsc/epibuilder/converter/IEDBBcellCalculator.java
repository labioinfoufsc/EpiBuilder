/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.converter;

import br.ufsc.epibuilder.entity.ReportBCell;
import br.ufsc.epibuilder.entity.SoftwareBCellMap;
import br.ufsc.epibuilder.entity.SoftwareBcellEnum;
import java.util.ArrayList;

/**
 *
 * @author renato
 */
public class IEDBBcellCalculator {

    public static ArrayList<ReportBCell> runKarplusSchulz(String sequence) {

        String AA = "KSGPDEQTNRALHVYIFCWM";

        float[] BNORM0 = {1.093f, 1.169f, 1.142f, 1.055f, 1.033f, 1.094f, 1.165f, 1.073f, 1.117f, 1.038f, 1.041f, 0.967f, 0.982f, 0.982f, 0.961f, 1.002f, 0.930f, 0.960f, 0.925f, 0.947f};
        float[] BNORM1 = {1.082f, 1.048f, 1.042f, 1.085f, 1.089f, 1.036f, 1.028f, 1.051f, 1.006f, 1.028f, 0.946f, 0.961f, 0.952f, 0.927f, 0.930f, 0.892f, 0.912f, 0.878f, 0.917f, 0.862f};
        float[] BNORM2 = {1.057f, 0.923f, 0.923f, 0.932f, 0.932f, 0.933f, 0.885f, 0.934f, 0.930f, 0.901f, 0.892f, 0.921f, 0.894f, 0.913f, 0.837f, 0.872f, 0.914f, 0.925f, 0.803f, 0.804f};
        float[] WT = {0.25f, 0.50f, 0.75f, 1.00f, 0.75f, 0.50f, 0.25f};

        int window = 7;
        ArrayList<Float> NAYB = new ArrayList<>();
        NAYB.add(0f);
        ArrayList<ReportBCell> list_values = new ArrayList<>();

        for (int i = 1; i < sequence.length() - 1; i++) {
            float val = 0;
            int i1 = AA.indexOf(sequence.substring(i - 1, i));
            int i2 = AA.indexOf(sequence.substring(i + 1, i + 2));

            if (i1 >= 10 || i2 >= 10) {
                val = 1;
            }

            if (i1 >= 10 && i2 >= 10) {
                val = 2;
            }

            NAYB.add(val);

        }
        NAYB.add(0f);

        for (int i = 4; i < sequence.length() - 3; i++) {
            float sum = 0;
            String peptide = "";
            for (int j = 0; j < window; j++) {
                String res = sequence.substring(i - 4 + j, i - 4 + j + 1);
                peptide += res;
                int index = AA.indexOf(res);

                if (NAYB.get(i - 4 + j) == 0) {
                    sum += BNORM0[index] * WT[j] / 4.0;
                }
                if (NAYB.get(i - 4 + j) == 1) {
                    sum += BNORM1[index] * WT[j] / 4.0;
                }
                if (NAYB.get(i - 4 + j) == 2) {
                    sum += BNORM2[index] * WT[j] / 4.0;
                }
            }
            int position = i;
            String residue = sequence.substring(position - 1, position);
            int startPos = i - 4 + 1;
            int endPos = startPos + window - 1;
            list_values.add(new ReportBCell(position, residue, startPos, endPos, peptide, sum));

        }
        return list_values;
    }

    public static ArrayList<ReportBCell> run(SoftwareBcellEnum method, String sequence) {

        ArrayList<ReportBCell> list_values = new ArrayList<>();

        if (method == SoftwareBcellEnum.KARPLUS_SCHULZ) {
            list_values = runKarplusSchulz(sequence);
        } else {
            int window = method.window;
            int center = method.center;

            int nRes = sequence.length();

            for (int i = 0; i < nRes + 1 - window; i++) {
                float product = 1;
                float total_scale = 0;
                String peptide = "";
                float average = 0;
                for (int j = 0; j < window; j++) {
                    String res = sequence.substring(i + j, i + j + 1);
                    peptide += res;
                    if (method == SoftwareBcellEnum.EMINI) {
                        product *= SoftwareBCellMap.get(SoftwareBcellEnum.EMINI, res);
                    } else {
                        total_scale += SoftwareBCellMap.get(method, res);
                    }
                }
                //Please, don't ask me about this!!! PLEASE
                if (method == SoftwareBcellEnum.EMINI) {
                    product *= Math.pow(0.37, -6);

                } else {
                    average = total_scale / window;
                }

                int position = i + center;
                String residue = sequence.substring(position - 1, position);
                int startPos = i + 1;
                int endPos = startPos + window - 1;

                float opt = (method == SoftwareBcellEnum.EMINI ? product : average);

                list_values.add(new ReportBCell(position, residue, startPos, endPos, peptide, opt));

            }

        }
        if (method == SoftwareBcellEnum.EMINI) {
            float avg = 0;
            int count = list_values.size();
            for (ReportBCell list_value : list_values) {
                avg += list_value.getScore();
            }
            avg = avg / count;
            for (ReportBCell list_value : list_values) {
                list_value.setScore(list_value.getScore() / avg);
            }
        }

        return list_values;
    }

    public static void print(ArrayList<ReportBCell> list_values) {
        System.out.println("Position\tResidue\tStart\tEnd\tPeptide\tScore");
        for (ReportBCell list_value : list_values) {
            System.out.printf("%s\t%s\t%s\t%s\t%s\t%.3f\n", list_value.getPosition(), list_value.getResidue(), list_value.getStartPos(), list_value.getEndPos(), list_value.getPeptide(), list_value.getScore());
        }
    }

    public static Float getScoreByPosition(int pos, ArrayList<ReportBCell> list_values) {

        for (ReportBCell list_value : list_values) {
            if (list_value.getPosition() == pos) {
                return list_value.getScore();
            }
        }
        return 0f;
    }
}
