/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 *
 * @author Debasis
 * Create two separate fields, one for the Pure one for the CodeMixed type...
 * Other fields stay the same as the base class...
 * The queries will be formulated to match terms both in the words (baseclass)
 * field and the two respective particular fields denoting the type
 * of the document.
 */
public class ClusteredTweetIndexer extends TweetIndexer {

    static final public String FIELD_CLASS_PURE = "pure_words";
    static final public String FIELD_CLASS_CDMIX = "mixed_words"; // 1 or 0
    
    public ClusteredTweetIndexer(String propFile) throws Exception {
        super(propFile);
        
        // The name of the output index path is different from the base class
        String indexPath = prop.getProperty("index.cluster");        
        indexDir = new File(indexPath);
    }
    
    @Override
    Document constructDoc(String id, String text) throws IOException {
        Document d = super.constructDoc(id, text);
        int codeMixed = Integer.parseInt(d.get(FIELD_CODEMIXED));
        
        String typeSpecFieldName = codeMixed==1? FIELD_CLASS_CDMIX : FIELD_CLASS_PURE;        
        Field typeSpecField = new Field(typeSpecFieldName, text,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES);
        d.add(typeSpecField);
        
        return d;
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java TweetIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            TweetIndexer indexer = new ClusteredTweetIndexer(args[0]);
            indexer.processAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }        
}
