package br.ufsc.epibuilder;

import br.ufsc.epibuilder.entity.Proteome;
import br.ufsc.epibuilder.entity.SoftwareBcellEnum;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author renato
 */
public class Parameters {

    public static SO OPERATIONAL_SYSTEM = SO.linux;
    public static double THRESHOLD_BEPIPRED = 0.6;
    public static int MIN_LENGTH_BEPIPRED = 10;
    public static int MAX_LENGTH_BEPIPRED = 30;
    public static int LENGTH_SEQUENCE_EMINI_PARKER = 10;
    public static File BEPIPRED_FILE = null;
    public static File EMINI_FILE = null;
    public static File PARKER_FILE = null;
    public static boolean NGLYC = false;
    public static double EMINI_TRESHOLD = 1;
    public static double PARKER_TRESHOLD = 1;
    public static ArrayList<Proteome> PROTEOMES = new ArrayList<>();
    public static LinkedHashMap<SoftwareBcellEnum, Double> MAP_SOFTWARES = new LinkedHashMap<>();
    public static String BASENAME = "";
    public static String BLAST_TASK = "blastp-short";
    public static double BLAST_IDENTITY = 90;
    public static double BLAST_COVER = 90;
    public static int BLAST_WORD_SIZE = 4;
    public static boolean SEARCH_BLAST = false;
    public static String MAKEBLASTDB_PATH = "makeblastdb";
    public static String BLASTP_PATH = "blastp";
    public static File FASTA;

    public enum SO {
        windows, linux, macos
    }

    public enum BEPIPRED_TYPE {
        CSV("csv"), FASTA("fasta");
        private final String name;

        private BEPIPRED_TYPE(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    public static BEPIPRED_TYPE BEPIPRED_INPUT = BEPIPRED_TYPE.FASTA;
    public static String DESTINATION_FOLDER = ".";
    public static boolean OUTPUT_FILE = false;
    public static boolean HIT_ACCESSION = true;

    static {
        Parameters.BASENAME = "run-" + String.format("%1$tF-%1$tH%1$tM%1$tS", Calendar.getInstance().getTime());
    }
}
