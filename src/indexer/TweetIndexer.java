/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Debasis
 */

public class TweetIndexer {
    Properties prop;
    File indexDir;
    IndexWriter writer;
    Analyzer analyzer;
    List<String> stopwords;
    int pass;
    HashMap<String, Byte> keywords;
    
    static final public String FIELD_ID = "id";
    static final public String FIELD_CODEMIXED = "codemixed"; // 1 or 0
    static final public String FIELD_ANALYZED_CONTENT = "words";  // Standard analyzer w/o stopwords.

    public TweetIndexer(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));                
        analyzer = getAnalyzer();
        String indexPath = prop.getProperty("index");        
        indexDir = new File(indexPath);
        
        loadCodeMixedTweetIds();
    }
    
    // Use EnglishAnallyzer here. In the split indices use bigrams for
    // the purpose of query sampling.
    Analyzer getAnalyzer() {
        if (analyzer != null)
            return analyzer;
        analyzer = TweetAnalyzerFactory.createAnalyzer(prop, true);
        return analyzer;
    }
    
    final void loadCodeMixedTweetIds() throws Exception {
        this.keywords = new HashMap<>();
        
        String codeMixedFile = prop.getProperty("codemix.keywords");
        FileReader fr = new FileReader(codeMixedFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
                
        while ( (line = br.readLine()) != null ) {
            keywords.put(line.trim(), null);
        }
        
        br.close();
        fr.close();
    }
    
    public Properties getProperties() { return prop; }
    
    void processAll() throws Exception {
        System.out.println("Indexing CodeMixed Tweet Collection...");
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(indexDir), iwcfg);
        
        indexAll();
        
        writer.close();
    }
    
    public File getIndexDir() { return indexDir; }
    
    void indexAll() throws Exception {
        if (writer == null) {
            System.err.println("Skipping indexing... Index already exists at " + indexDir.getName() + "!!");
            return;
        }
        
        File topDir = new File(prop.getProperty("coll"));
        indexDirectory(topDir);        
    }
    
    private void indexDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        for (int i=0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                System.out.println("Indexing directory " + f.getName());
                indexDirectory(f);  // recurse
            }
            else
                indexFile(f);
        }
    }
    
    boolean isCodeMixed(String text) {  // a very simplistic way of checking code mixing
        String[] tokens = text.split("\\s+");
        for (String word : tokens) {
            String nword = word.toLowerCase();
            if (keywords.containsKey(nword))
                return true;
        }
        return false;
    }
    
    Document constructDoc(String id, String text) throws IOException {        
        String codeMixed = isCodeMixed(text)? "1" : "0";
        Document doc = new Document();
        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_CODEMIXED, codeMixed, Field.Store.YES, Field.Index.NOT_ANALYZED));        
        doc.add(new Field(FIELD_ANALYZED_CONTENT, text,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        
        return doc;
    }

    void indexFile(File file) throws Exception {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        Document doc;

        // skip swp files
        String fileName = file.getName();
        if (fileName.charAt(0) == '.')
            return;
        
        System.out.println("Indexing file: " + fileName);
        
        // Each line is a tweet document
        while ((line = br.readLine()) != null) {
            
            int indexOfFirstSpace = line.indexOf(' ');
            if (indexOfFirstSpace < 0) {
                System.err.println("Skipping doc: " + line);
                continue;
            }
            
            String id = line.substring(0, indexOfFirstSpace);
            int indexOfSecondSpace = line.indexOf(' ', indexOfFirstSpace+1);
            if (indexOfSecondSpace < 0) {
                System.err.println("Skipping doc: " + line);
                continue;
            }
            
            String text = line.substring(indexOfSecondSpace);            
                    
            doc = constructDoc(id, text);
            writer.addDocument(doc);
        }
        
        br.close();
        fr.close();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java TweetIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            TweetIndexer indexer = new TweetIndexer(args[0]);
            indexer.processAll();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
