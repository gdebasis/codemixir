/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.util.Version;

/**
 *
 * @author Debasis
 */
public class TweetAnalyzerFactory {
    static List<String> stopList;
    
    private static List<String> buildStopwordList(Properties prop) {
        if (stopList != null)
            return stopList;
        
        List<String> stopwords = new ArrayList<>();
        String stopFile = prop.getProperty("stopfile");        
        String line;

        try (FileReader fr = new FileReader(stopFile);
            BufferedReader br = new BufferedReader(fr)) {
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        stopList = stopwords;
        return stopwords;
    }

    public static Analyzer createAnalyzer(Properties prop, boolean uniGramsOnly) {        
        Analyzer eanalyzer = new TweetAnalyzer(
            prop, uniGramsOnly,
            Version.LUCENE_4_9,
            StopFilter.makeStopSet(
                Version.LUCENE_4_9, buildStopwordList(prop))); // default analyzer
        return eanalyzer;        
    }
    
}


