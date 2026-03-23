package it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("ivassTokenAnalyzer")
public class InsuranceCompanyTokenAnalyzer extends Analyzer {

    public InsuranceCompanyTokenAnalyzer() {
        log.trace("Initializing {}", InsuranceCompanyTokenAnalyzer.class.getSimpleName());
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final StandardTokenizer tokenizer = new StandardTokenizer();
        final LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(tokenizer);
        final StopFilter stopFilter = new StopFilter(lowerCaseFilter, ItalianAnalyzer.getDefaultStopSet());
        final ASCIIFoldingFilter asciiFoldingFilter = new ASCIIFoldingFilter(stopFilter);
        final NGramTokenFilter nGramTokenFilter = new NGramTokenFilter(asciiFoldingFilter, 3, 5, true);
        return new TokenStreamComponents(tokenizer, nGramTokenFilter);
  }

}
