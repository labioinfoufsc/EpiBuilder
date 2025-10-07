/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.proteomics;

import org.biojava.nbio.aaproperties.PeptideProperties;
import org.biojava.nbio.aaproperties.PeptidePropertiesImpl;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.ProteinSequence;

/**
 *
 * @author renato
 */
public class ProteomicCalculator {

    private static PeptidePropertiesImpl peptideProperties = new PeptidePropertiesImpl();

    public static double getMolecularWeight(String sequence) throws CompoundNotFoundException {
        return peptideProperties.getMolecularWeight(new ProteinSequence(sequence));
    }

    public static double getIsoelectricPoint(String sequence) throws CompoundNotFoundException {
        return peptideProperties.getIsoelectricPoint(new ProteinSequence(sequence));
    }

    public static double getHydropathy(String sequence) throws CompoundNotFoundException {
        return peptideProperties.getAvgHydropathy(new ProteinSequence(sequence));
    }
}
