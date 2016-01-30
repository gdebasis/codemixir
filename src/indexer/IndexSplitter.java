/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Debasis
 */
public class IndexSplitter extends TweetIndexer {
    IndexWriter pureIndexWriter;
    IndexWriter mixedIndexWriter;
    
    public IndexSplitter(String propFile) throws Exception {
        super(propFile);
        
        File pureIndexDir = new File(prop.getProperty("splitindex.pure"));
        File mixedIndexDir = new File(prop.getProperty("splitindex.mixed"));
        
        IndexWriterConfig iwcfg_pure = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
        iwcfg_pure.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriterConfig iwcfg_mixed = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
        iwcfg_mixed.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        
        pureIndexWriter = new IndexWriter(FSDirectory.open(pureIndexDir), iwcfg_pure);
        mixedIndexWriter = new IndexWriter(FSDirectory.open(mixedIndexDir), iwcfg_mixed);        
    }
    
    public void split() throws Exception {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
        final int numDocs = reader.numDocs();
        IndexWriter pWriter; // pointer variable
        
        for (int i = 0; i < numDocs; i++) {
            Document d = reader.document(i);            
            pWriter = d.get(FIELD_CODEMIXED).equals("1")? mixedIndexWriter : pureIndexWriter;
            pWriter.addDocument(d);
        }
        
        reader.close();
        pureIndexWriter.close();
        mixedIndexWriter.close();
    }
    
    @Override
    Analyzer getAnalyzer() {
        if (analyzer != null)
            return analyzer;
        analyzer = TweetAnalyzerFactory.createAnalyzer(prop, false); // We want bigrams here
        return analyzer;
    }
    
    public static void main(String[] args) {
        
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java IndexSplitter <prop-file>");
            args[0] = "init.properties";
        }

        try {
            IndexSplitter indexer = new IndexSplitter(args[0]);
            indexer.split();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
