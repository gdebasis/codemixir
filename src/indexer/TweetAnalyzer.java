/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.util.Version;

/**
 *
 * @author Debasis
 */
public class TweetAnalyzer extends Analyzer {

    boolean toStem;
    boolean uniGramsOnly;
    Version matchVersion;
    CharArraySet stopSet;
    
    public TweetAnalyzer(Properties prop, boolean uniGramsOnly, Version matchVersion, CharArraySet stopSet) {
        toStem = Boolean.parseBoolean(prop.getProperty("stem", "false"));
        this.matchVersion = matchVersion;
        this.uniGramsOnly = uniGramsOnly;
        this.stopSet = stopSet;
    }
    
    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final Tokenizer tokenizer = new UAX29URLEmailTokenizer(matchVersion, reader);
        
        TokenStream tokenStream = new StandardFilter(matchVersion, tokenizer);
        tokenStream = new LowerCaseFilter(matchVersion, tokenStream);
        tokenStream = new StopFilter(matchVersion, tokenStream, stopSet);
        tokenStream = new URLFilter(matchVersion, tokenStream); // remove URLs
        
        if (!uniGramsOnly)
            tokenStream = new ShingleFilter(tokenStream, 2);  // index bigrams as well...
        
        if (toStem)
            tokenStream = new PorterStemFilter(tokenStream);
        
        return new Analyzer.TokenStreamComponents(tokenizer, tokenStream);
    }    
}


class URLFilter extends FilteringTokenFilter {

    TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

    public URLFilter(Version version, TokenStream in) {
        super(version, in);
    }
    
    @Override
    protected boolean accept() throws IOException {
        return !(typeAttr.type() == UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.URL]);
    }    
}



