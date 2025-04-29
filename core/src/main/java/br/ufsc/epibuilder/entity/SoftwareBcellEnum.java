/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity;

/**
 *
 * @author renato
 */
public enum SoftwareBcellEnum {
    PARKER("Parker", 7, 4),
    CHOU_FOSMAN("Chou Fosman", 7, 4), 
    EMINI("Emini", 6, 3),
    KARPLUS_SCHULZ("Karplus Schulz", 7, 4), 
    KOLASKAR("Kolaskar", 7, 4), 
    BEPIPRED("BepiPred-3.0", 0, 0);

    public int window;
    public int center;
    public String description;

    SoftwareBcellEnum(String description, int window, int center) {
        this.description = description;
        this.window = window;
        this.center = center;
    }
}
