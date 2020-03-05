import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.edu.mimuw.gc401929.core.SimpleAdder.add;
import static pl.edu.mimuw.gc401929.core.SimpleAdder.addPositive;
//import static pl.edu.mimuw.gc401929.core.SimpleAdder.add;
//import static pl.edu.mimuw.gc401929.core.SimpleAdder.addPositive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.morfologik.MorfologikAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.junit.jupiter.api.Test;

public class ExampleTest {

	@Test
	void testAddition() {
		assertThat(add(1, 1)).isEqualTo(2);
		assertThat(addPositive(2, 2)).isNotEqualTo(42);
	}

	@Test
	void testIllegalArguments() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> addPositive(1, -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> addPositive(1337, 0))
			.withMessageContaining("Arguments must be positive");
	}

	@Test
	void testLanguageDetector() throws IOException, TikaException {
		try (InputStream stream = ExampleTest.class.getResourceAsStream("rblarba-pl.pdf")) {
			assertThat(stream).isNotNull();

			Tika tika = new Tika();
			String content = tika.parseToString(stream);
			assertThat(content).contains("blabaliza");

			LanguageDetector detector = new OptimaizeLangDetector();
			detector.loadModels();
			assertTrue(detector.hasModel("pl"));

			LanguageResult result = detector.detect(content);
			assertTrue(result.isReasonablyCertain());
			assertThat(result.isLanguage("pl"));
		}

	}

	@Test
	void testSearchTermInMemoryIndex() throws IOException, InvalidTokenOffsetsException {
		Document doc = new Document();
		String text = String.join("", Collections.nCopies(1000, "W czasie suszy szosa sucha.\n"))
				+ "Król Karol kupił królowej Karolinie korale koloru koralowego.\n"
				+ String.join("", Collections.nCopies(1000, "W czasie suszy szosa sucha.\n"));
		assertThat(text).contains("Karolinie").contains("szosa");
		doc.add(new TextField("body", text, Field.Store.YES));

		Analyzer analyzer = new MorfologikAnalyzer();

		MemoryIndex index = MemoryIndex.fromDocument(doc, analyzer, true, true);
		IndexSearcher searcher = index.createSearcher();

		Query query = new TermQuery(new Term("body", "Karolina"));
		assertThat(searcher.count(query)).isEqualTo(1);

		TopDocs docs = searcher.search(query, 10);
		assertThat(docs.scoreDocs).hasSize(1).extracting(d -> d.doc).allSatisfy(Objects::nonNull);
		
		TokenStream tokens = analyzer.tokenStream("body", doc.get("body"));
		QueryScorer scorer = new QueryScorer(query);
		/* This highlighter uses SimpleHTMLFormatter that wraps matched fragment with <B> tag. Hence the assertion below. */
		Highlighter highlighter = new Highlighter(scorer);
		
		String highlighted = highlighter.getBestFragment(tokens, text);
		assertThat(highlighted).hasSizeLessThan(200).contains("<B>Karolinie</B>");
	}

	@Test
	void testxD() {
		assertThat(Dummy.f(0)).isEqualTo("xD");
	}

}
