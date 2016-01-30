/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retrievability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.queries.function.valuesource.NumDocsValueSource;

/**
 *
 * @author Debasis
 */
class CollLevelRetrievabilityMeasure {
    List<RetrievabilityScore> rtrScores;
    float avgRetScore;
    float expectedRetScore;
    float giniCoeff;
    int numDocs;

    public CollLevelRetrievabilityMeasure() {
        rtrScores = new ArrayList<>();
    }
    
    void computeAvg() {
        // Avg Retr
        avgRetScore = 0;
        rtrScores.stream().forEach((sc) -> {
            avgRetScore += sc.score;
        });        
        avgRetScore /= (float)numDocs;
    }
    
    void computeExpctedRet() {
        // Expected value (assume sorted by scores)
        int prevScore = 0, ri, count = 0;
        expectedRetScore = 0;
        for (int i = 0; i < numDocs; i++) {
            ri = rtrScores.get(i).score;
            if ((prevScore>0) && ri!=prevScore) {
                // score changes
                float normScore = count/(float)numDocs * prevScore; // P[score=<prevScore>]
                expectedRetScore += normScore;
                count = 0;
            }
            prevScore = ri;
            count++;
        }        
    }
    
    void computeGiniCoeff() {
        // Gini coeff.
        float numerator = 0;
        int ri;
        int denom = 0;
        for (int i=0; i < numDocs; i++) {
            ri = rtrScores.get(i).score;
            numerator += (numDocs+1-i)*ri;
            denom += ri;
        }
        giniCoeff = (numDocs+1 -2*numerator/(float)denom)/(float)numDocs;
    }
    
    public void compute(String codeMixed) {

        numDocs = rtrScores.size();
        Collections.sort(rtrScores); // ascending order of the scores
        
        computeAvg();
        computeExpctedRet();
        computeGiniCoeff();
        
        System.out.println(codeMixed + "\t" +
                avgRetScore + "\t" +
                expectedRetScore + "\t" +
                giniCoeff);

    }
}

public class RetrievabilityScoreReporter {
    Properties prop;
    static final int NUM_SIMS = RetrievabilityFinder.sims.length;
    CollLevelRetrievabilityMeasure[] rtrMeasures;  // one for each sim type
    
    public RetrievabilityScoreReporter(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
        rtrMeasures = new CollLevelRetrievabilityMeasure[2]; // for pure and mixed
    }
    
    void initSimMeasure(String simName) throws Exception {
        
        for (int i = 0; i < 2; i++)
            rtrMeasures[i] = new CollLevelRetrievabilityMeasure();
        
        String fullFileName = prop.getProperty("retrievability.outdir") + "/" + simName + ".txt";
        FileReader fr = new FileReader(fullFileName);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            String docName = tokens[0];
            int docId = Integer.parseInt(tokens[1]);
            int codeMixed = Integer.parseInt(tokens[2]);
            int score = Integer.parseInt(tokens[3]);
            rtrMeasures[codeMixed].rtrScores.add(
                    new RetrievabilityScore(
                        docName, docId, (codeMixed==1), score));
        }
        br.close();
        fr.close();
    }
    
    void computeForAllSims() throws Exception {
        final String[] codeMixed = { "pure", "mixed" };
        
        for (int i = 0; i < RetrievabilityFinder.sims.length; i++) {
            initSimMeasure(RetrievabilityFinder.fileNames[i]);
            System.out.println(RetrievabilityFinder.fileNames[i]);
            for (int j = 0; j < 2; j++) {
                rtrMeasures[j].compute(codeMixed[j]);
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java RetrievabilityScoreReporter <prop-file>");
            args[0] = "init.properties";
        }

        try {
            RetrievabilityScoreReporter reporter = new RetrievabilityScoreReporter(args[0]);
            reporter.computeForAllSims();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }            
}
