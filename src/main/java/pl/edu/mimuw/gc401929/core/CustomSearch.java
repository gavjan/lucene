package pl.edu.mimuw.gc401929.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class CustomSearch {
    private static final String INDEX_DIR = System.getProperty("user.home") + File.separatorChar + ".index";
    private static final int TERM = 1;
    private static final int PHRASE = 2;
    private static final int FUZZY = 3;
    private static final int MAX_RESULTS = 10;

    public static TopDocs getFoundDocs(String textToFind, int limit, IndexSearcher searcher, int mode) throws Exception {
        TopDocs foundDocs = null;
        switch (mode) {
            case TERM:
                foundDocs = searchContent(textToFind, searcher, limit);
                break;
            case PHRASE:
                foundDocs = searchPhrase(textToFind, searcher, limit);
                break;
            case FUZZY:
                foundDocs = searchFuzzy(textToFind, searcher, limit);
                break;
        }
        return foundDocs;
    }

    public static void search(String textToFind, int limit, int mode) throws Exception {
        //Create lucene searcher. It search over a single IndexReader.
        IndexSearcher searcher = createSearcher();

        TopDocs foundDocs = getFoundDocs(textToFind, limit, searcher, mode);

        //Total found documents
        System.out.println("Files Count: " + foundDocs.scoreDocs.length);

        //Print out the path of files which have searched term
        for (ScoreDoc sd : foundDocs.scoreDocs) {
            Document d = searcher.doc(sd.doc);
            System.out.println("\n" + d.get("path"));
        }
    }

    private static TopDocs searchContent(String textToFind, IndexSearcher searcher, int limit) throws Exception {
        //Create search query
        QueryParser qp = new QueryParser("contents", new StandardAnalyzer());
        Query query = qp.parse(textToFind);

        return searcher.search(query, limit);
    }

    private static TopDocs searchPhrase(String sentence, IndexSearcher searcher, int limit) throws Exception {
        //Create search query
        String[] words = sentence.split(" ");
        Query query = new PhraseQuery(1, "contents", words);

        return searcher.search(query, limit);
    }

    private static TopDocs searchFuzzy(String textToFind, IndexSearcher searcher, int limit) throws Exception {
        //Create search query
        Term term = new Term("contents", textToFind);
        Query query = new FuzzyQuery(term);

        return searcher.search(query, limit);
    }


    private static IndexSearcher createSearcher() throws IOException {
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));

        //It is an interface for accessing a point-in-time view of a lucene index
        IndexReader reader = DirectoryReader.open(dir);

        //Index searcher
        return new IndexSearcher(reader);
    }

    public static void highlight(String line, int limit, int mode, boolean color) throws IOException, ParseException, InvalidTokenOffsetsException {
        //Get directory reference
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));

        //Index reader - an interface for accessing a point-in-time view of a lucene index
        IndexReader reader = DirectoryReader.open(dir);

        //Create lucene searcher. It search over a single IndexReader.
        IndexSearcher searcher = new IndexSearcher(reader);

        //analyzer with the default stop words
        Analyzer analyzer = new StandardAnalyzer();

        //Query parser to be used for creating TermQuery
        QueryParser qp = new QueryParser("contents", analyzer);

        //Create the query
        Query query = qp.parse(line);

        //Search the lucene documents

        TopDocs hits = null;
        try {
            hits = getFoundDocs(line, limit, searcher, mode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Uses HTML &lt;B&gt;&lt;/B&gt; tag to highlight the searched terms
        Formatter formatter = new SimpleHTMLFormatter();

        //It scores text fragments by the number of unique query terms found
        //Basically the matching score in layman terms
        QueryScorer scorer = new QueryScorer(query);

        //used to markup highlighted terms found in the best sections of a text
        Highlighter highlighter = new Highlighter(formatter, scorer);

        //It breaks text up into same-size texts but does not split up spans
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, MAX_RESULTS);

        //set fragmenter to highlighter
        highlighter.setTextFragmenter(fragmenter);
        System.out.println("File count: " + hits.scoreDocs.length);

        //Iterate over found results
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            int docid = hits.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            String title = doc.get("path");

            //Printing - to which document result belongs
            System.out.println("\n" + title + ": ");

            //Get stored text from found document
            String text = doc.get("contents");

            //Create token stream
            TokenStream stream = TokenSources.getAnyTokenStream(reader, docid, "contents", analyzer);

            //Get highlighted text fragments
            String[] frags = highlighter.getBestFragments(stream, text, MAX_RESULTS);
            System.out.print("\n");
            boolean first = true;
            for (String frag : frags) {
                if (!first) System.out.print(" ... ");
                first = false;
                if (color) {
                    System.out.print(frag.replaceAll("<B>([^<]*)</B>", "\033[31m" + "$1" + "\033[0m"));
                } else {
                    System.out.print(frag.replaceAll("<B>([^<]*)</B>", "$1"));
                }
            }
            System.out.print("\n");
        }
    }
}