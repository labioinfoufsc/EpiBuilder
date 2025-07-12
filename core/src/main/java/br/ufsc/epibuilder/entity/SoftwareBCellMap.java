/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 *
 * @author renato
 */
public class SoftwareBCellMap {

    public static Table<SoftwareBcellEnum, String, Float> scale = HashBasedTable.create();

    static {
        scale.put(SoftwareBcellEnum.PARKER, "A", 2.1f);
        scale.put(SoftwareBcellEnum.PARKER, "C", 1.4f);
        scale.put(SoftwareBcellEnum.PARKER, "D", 10f);
        scale.put(SoftwareBcellEnum.PARKER, "E", 7.8f);
        scale.put(SoftwareBcellEnum.PARKER, "F", -9.2f);
        scale.put(SoftwareBcellEnum.PARKER, "G", 5.7f);
        scale.put(SoftwareBcellEnum.PARKER, "H", 2.1f);
        scale.put(SoftwareBcellEnum.PARKER, "I", -8f);
        scale.put(SoftwareBcellEnum.PARKER, "K", 5.7f);
        scale.put(SoftwareBcellEnum.PARKER, "L", -9.2f);
        scale.put(SoftwareBcellEnum.PARKER, "M", -4.2f);
        scale.put(SoftwareBcellEnum.PARKER, "N", 7f);
        scale.put(SoftwareBcellEnum.PARKER, "P", 2.1f);
        scale.put(SoftwareBcellEnum.PARKER, "Q", 6f);
        scale.put(SoftwareBcellEnum.PARKER, "R", 4.2f);
        scale.put(SoftwareBcellEnum.PARKER, "S", 6.5f);
        scale.put(SoftwareBcellEnum.PARKER, "T", 5.2f);
        scale.put(SoftwareBcellEnum.PARKER, "V", -3.7f);
        scale.put(SoftwareBcellEnum.PARKER, "W", -10f);
        scale.put(SoftwareBcellEnum.PARKER, "Y", -1.9f);

        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "A", 0.66f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "C", 1.19f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "D", 1.46f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "E", 0.74f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "F", 0.6f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "G", 1.56f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "H", 0.95f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "I", 0.47f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "K", 1.01f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "L", 0.59f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "M", 0.6f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "N", 1.56f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "P", 1.52f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "Q", 0.98f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "R", 0.95f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "S", 1.43f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "T", 0.96f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "V", 0.5f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "W", 0.96f);
        scale.put(SoftwareBcellEnum.CHOU_FOSMAN, "Y", 1.14f);

        scale.put(SoftwareBcellEnum.EMINI, "A", 0.49f);
        scale.put(SoftwareBcellEnum.EMINI, "C", 0.26f);
        scale.put(SoftwareBcellEnum.EMINI, "D", 0.81f);
        scale.put(SoftwareBcellEnum.EMINI, "E", 0.84f);
        scale.put(SoftwareBcellEnum.EMINI, "F", 0.42f);
        scale.put(SoftwareBcellEnum.EMINI, "G", 0.48f);
        scale.put(SoftwareBcellEnum.EMINI, "H", 0.66f);
        scale.put(SoftwareBcellEnum.EMINI, "I", 0.34f);
        scale.put(SoftwareBcellEnum.EMINI, "K", 0.97f);
        scale.put(SoftwareBcellEnum.EMINI, "L", 0.4f);
        scale.put(SoftwareBcellEnum.EMINI, "M", 0.48f);
        scale.put(SoftwareBcellEnum.EMINI, "N", 0.78f);
        scale.put(SoftwareBcellEnum.EMINI, "P", 0.75f);
        scale.put(SoftwareBcellEnum.EMINI, "Q", 0.84f);
        scale.put(SoftwareBcellEnum.EMINI, "R", 0.95f);
        scale.put(SoftwareBcellEnum.EMINI, "S", 0.65f);
        scale.put(SoftwareBcellEnum.EMINI, "T", 0.7f);
        scale.put(SoftwareBcellEnum.EMINI, "V", 0.36f);
        scale.put(SoftwareBcellEnum.EMINI, "W", 0.51f);
        scale.put(SoftwareBcellEnum.EMINI, "Y", 0.76f);

        scale.put(SoftwareBcellEnum.KOLASKAR, "A", 1.064f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "C", 1.412f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "D", 0.866f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "E", 0.851f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "F", 1.091f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "G", 0.874f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "H", 1.105f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "I", 1.152f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "K", 0.93f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "L", 1.25f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "M", 0.826f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "N", 0.776f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "P", 1.064f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "Q", 1.015f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "R", 0.873f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "S", 1.012f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "T", 0.909f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "V", 1.383f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "W", 0.893f);
        scale.put(SoftwareBcellEnum.KOLASKAR, "Y", 1.161f);

    }

    public static float get(SoftwareBcellEnum software, String aa) {
        return scale.get(software, aa);
    }

    public static void main(String[] args) {
        System.out.println(get(SoftwareBcellEnum.PARKER,"A"));
    }
}
