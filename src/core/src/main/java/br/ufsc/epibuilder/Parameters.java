package br.ufsc.epibuilder;

import br.ufsc.epibuilder.entity.Proteome;
import br.ufsc.epibuilder.entity.SoftwareBcellEnum;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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

    public static double THRESHOLD_BEPIPRED = 0.6;
    public static int MIN_LENGTH_BEPIPRED = 10;
    public static int MAX_LENGTH_BEPIPRED = 30;
    public static int LENGTH_SEQUENCE_EMINI_PARKER = 10;
    public static File EMINI_FILE = null;
    public static File PARKER_FILE = null;
    public static boolean NGLYC = false;
    public static double EMINI_TRESHOLD = 1;
    public static double PARKER_TRESHOLD = 1;
    public static ArrayList<Proteome> PROTEOMES = new ArrayList<>();
    public static LinkedHashMap<SoftwareBcellEnum, Double> MAP_SOFTWARES = new LinkedHashMap<>();
    public static String BLAST_TASK = "blastp-short";
    public static Integer BLAST_IDENTITY = 90;
    public static Integer BLAST_COVER = 90;
    public static int BLAST_WORD_SIZE = 4;
    public static boolean SEARCH_BLAST = false;
    public static String MAKEBLASTDB_PATH = "makeblastdb";
    public static String BLASTP_PATH = "blastp_custom";
    public static File INPUT;
    public static HashMap<String, String> MAP_PROTEIN_DESCRIPTION = new HashMap<>();
    public static HashMap<String, String> MAP_PROTEIN_LOCALIZATION = new HashMap<>();
    public static String DESTINATION_FOLDER = "./epibuilder-results";
    public static boolean OUTPUT_FILE = false;
    public static boolean HIT_ACCESSION = true;
    public static LocalizationType LOCALIZATION_TYPE = null;
    public static File LOCALIZATION_FILE;
    public static File DESCRIPTION_FILE;

    public enum FileType {
        csv, fasta
    }
    public enum LocalizationType{
        animal, fungi, plant, arch, gram_pos, gram_neg
    }


}
