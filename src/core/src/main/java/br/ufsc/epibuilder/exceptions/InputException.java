/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.exceptions;

/**
 *
 * @author renato
 */
public class InputException extends Exception{

    public InputException(Exception e) {
        super("Error 1 - Convertion error.\nPlease check the input files. There are 2 options of Bepipred-2 input files: online (param -o) and IEDB BCELL Antibody Epitope Prediction (download and install from https://downloads.iedb.org/tools/bcell/LATEST/.");
        e.printStackTrace();
    }
    
}
