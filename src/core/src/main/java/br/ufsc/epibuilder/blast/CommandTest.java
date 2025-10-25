/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.blast;

import java.io.IOException;

/**
 *
 * @author renato
 */
public class CommandTest {

    public static boolean test(String cmd)  {
        try {
            Runtime.getRuntime().exec(cmd);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
