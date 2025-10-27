package br.ufsc.epibuilder.util;

import org.biojava.nbio.aaproperties.PeptideProperties;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastaUtils {
    public static final boolean doesSequenceContainInvalidChar(String sequence, Set<Character> cSet) {
        for(char c : sequence.toCharArray()) {
            if (!cSet.contains(c)) {
                return true;
            }
        }

        return false;
    }
    public static boolean isSequenceValid(String sequence) {
        return !doesSequenceContainInvalidChar(sequence, PeptideProperties.standardAASet);
    }

    public static String extractName(String text) {
        // Verifica se a string contém 'OS='
        if (text.contains("OS=")) {
            // Expressão regular para capturar texto entre o primeiro espaço e 'OS='
            Pattern pattern = Pattern.compile("\\s(.*?)\\sOS=");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        // Se 'OS=' não estiver presente, verifica se a string contém '['
        else if (text.contains("[")) {
            // Expressão regular para capturar texto entre o primeiro espaço e o caractere '['
            Pattern pattern = Pattern.compile("\\s(.*?)\\s\\[");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        // Se nenhuma das condições for satisfeita, retorna tudo após o primeiro espaço
        else {
            Pattern pattern = Pattern.compile("\\s(.*)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        // Retorna a string inteira se nenhum espaço for encontrado
        return text;
    }
}
