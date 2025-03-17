import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class Lemmatizer {
    private final POSTaggerME posTagger;
    private final LemmatizerME lemmatizer;

    public Lemmatizer() throws Exception {
        try (InputStream posModelIn = getClass().getResourceAsStream("/opennlp-pl-pos-maxent.bin");
             InputStream lemmatizerModelIn = getClass().getResourceAsStream("/opennlp-pl-lemmatizer.bin")) {
            if (posModelIn == null || lemmatizerModelIn == null) {
                throw new FileNotFoundException("Model files not found");
            }
            POSModel posModel = new POSModel(posModelIn);
            posTagger = new POSTaggerME(posModel);

            LemmatizerModel lemmatizerModel = new LemmatizerModel(lemmatizerModelIn);
            lemmatizer = new LemmatizerME(lemmatizerModel);
        }
    }

    public String[] lemmatizeSentence(String sentence) {
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(sentence);
        String[] tags = posTagger.tag(tokens);
        return lemmatizer.lemmatize(tokens, tags);
    }

//
}