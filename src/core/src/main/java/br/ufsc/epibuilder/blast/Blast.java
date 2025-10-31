/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.blast;

import br.ufsc.epibuilder.Parameters;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author renato
 */
public class Blast {

    private String name;
    private File file;
    ArrayList<BlastPeptide> peps = new ArrayList<>();

    public Blast(String name, File file) throws FileNotFoundException {
        this.name = name;
        this.file = file;
        Scanner s = new Scanner(file);
        s.nextLine();
        while (s.hasNext()) {
            BlastPeptide pep = new BlastPeptide(s.nextLine());
            peps.add(pep);
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    private void clear(double id, double cov) {
        ArrayList<BlastPeptide> res = new ArrayList<BlastPeptide>();
        for (BlastPeptide pep : peps) {
            if (pep.getIdentity() >= id && pep.getCover() >= cov) {
                res.add(pep);
            }
        }
        peps.clear();
        peps.addAll(res);
    }

    private int getCount(String peptideAccessId, TreeMap<String, ArrayList<BlastPeptide>> map) {
        if (map.get(peptideAccessId) != null) {
            return map.get(peptideAccessId).size();
        }
        return 0;
    }

    private String getIds(String peptideAccessId, TreeMap<String, ArrayList<BlastPeptide>> map) {
        String res = "-";
        if (map.get(peptideAccessId) != null && Parameters.HIT_ACCESSION) {
            ArrayList<BlastPeptide> pep = map.get(peptideAccessId);
            return Joiner.on(",").join(pep);
        }
        return res;
    }

    private TreeMap<String, ArrayList<BlastPeptide>> getMap() {
        TreeMap<String, ArrayList<BlastPeptide>> map = new TreeMap<>();
        for (BlastPeptide pep : peps) {
            if (map.get(pep.getPeptideAccessId()) == null) {
                map.put(pep.getPeptideAccessId(), new ArrayList<>());
            }
            map.get(pep.getPeptideAccessId()).add(pep);
        }
        return map;
    }

    public String getListReport(double identity, double cover) {
        StringBuilder sb = new StringBuilder();
        //remove duplicated ids
        TreeSet<String> listIds = new TreeSet<>();
        for (BlastPeptide pep : peps) {
            listIds.add(pep.getPeptideAccessId());
        }
        //remove hits less than identity and cover
        clear(identity, cover);

        TreeMap<String, ArrayList<BlastPeptide>> map = getMap();

        sb.append("Peptide\tAccession\tCount\tId\n");
        for (String listId : listIds) {
            sb.append(String.format("%s\t%s\t%s\n", listId, getCount(listId.trim(), map), getIds(listId.trim(), map)));
        }

        return sb.toString();
    }
}
