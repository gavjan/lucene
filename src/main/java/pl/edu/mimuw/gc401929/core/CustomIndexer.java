package pl.edu.mimuw.gc401929.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.beust.jcommander.Parameter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.min;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;
import static pl.edu.mimuw.gc401929.core.CrunchifyData.crunchifyReadFromFile;

public class CustomIndexer {

    private static final String INDEX_DIR = System.getProperty("user.home") + File.separatorChar + ".index";
    private static CrunchifyData crunchify;
    private static CustomIndexer main;
    private static IndexWriter writer = null;
    @Parameter(names = "--help", help = true, description = "Displays help information")
    private boolean help;

    @Parameter(names = "--purge", description = "Deletes all documents from the index and exits")
    private boolean purge;

    @Parameter(names = "--add", description = "Adds the directory to the indexed catalogs set")
    private String add;

    @Parameter(names = "--rm", description = "Removes the directory from the indexed catalog set")
    private String rm;

    @Parameter(names = "--reindex", description = "Causes re-indexing of catalog set ")
    private boolean reindex;

    @Parameter(names = "--list", description = "Prints the list of files in the catalog set")
    private boolean list;

    public static void main(String... argv) throws TikaException, IOException {
        main = new CustomIndexer();
        JCommander jct = JCommander.newBuilder().addObject(main).build();
        jct.parse(argv);
        if (main.help) {
            jct.usage();
            return;
        }

        crunchify = crunchifyReadFromFile();
        // If the file doesn't exist
        if (crunchify == null) {
            crunchify = new CrunchifyData();
            crunchify.setCatalog(new ArrayList<String>());
        }
        if (main.add != null) {
            log("Catalog consists of: " + crunchify.getCatalog());
            boolean exists = Files.exists(Paths.get(main.add));
            if (!exists) err("Can't index non-existent directory");
            if (exists && crunchify.addPath(main.add)) {
                addIndex(main.add, main.add);
            }
        } else if (main.rm != null) {
            if (crunchify.deletePath(main.rm))
                deleteDocumentsCatalog(main.rm);
            else
                log("Given directory isn't being monitored");
        } else if (main.reindex) {
            FileUtils.cleanDirectory(new File(INDEX_DIR));
            log("Deleted everything in INDEX_DIR");
            ArrayList<String> dirs = crunchify.getCatalog();
            for (String x : dirs) {
                addIndex(x, x);
            }
            log("Re-indexed everything back");
        } else if (main.purge) {
            FileUtils.cleanDirectory(new File(INDEX_DIR));
            log("Deleted everything in INDEX_DIR");
            return;
        } else if (main.list) {
            ArrayList<String> dirs = crunchify.getCatalog();
            for (String x : dirs) {
                System.out.println(x);
            }
        } else {

            ArrayList<String> dirs = crunchify.getCatalog();
            if (dirs.isEmpty())
                err("No catalogs currently being monitored");
            else
                new WatchDir(dirs).processEvents();
        }
        crunchify.WriteToFile();
    }

    private static IndexWriter getIndexWriter() {
        if (writer != null) return writer;
        String indexPath = INDEX_DIR;
        Directory dir;
        try {
            dir = FSDirectory.open(Paths.get(indexPath));

            //analyzer with the default stop words
            Analyzer analyzer = new StandardAnalyzer();

            //IndexWriter Configuration
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(CREATE_OR_APPEND);

            //IndexWriter writes new index files to the directory
            writer = new IndexWriter(dir, iwc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer;
    }

    private static void addIndex(String toIndex, String docsPath) throws TikaException {
        //Input folder: docsPath
        final Path path = Paths.get(toIndex);


        //org.apache.lucene.store.Directory instance

        IndexWriter writer;
        writer = getIndexWriter();
        //Its recursive method to iterate all files and directories
        indexDocs(writer, path, docsPath);

        try {
            writer.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void indexDocs(final IndexWriter writer, Path path, String docsPath) throws TikaException {
        //Directory?
        if (Files.isDirectory(path)) {
            //Iterate directory
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            //Index this file
                            indexDoc(writer, file, docsPath);
                        } catch (TikaException ioe) {
                            ioe.printStackTrace();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                err("IOException, Skipping file");
            }
        } else {
            //Index this file

            indexDoc(writer, path, docsPath);
        }
    }

    private static void indexDoc(IndexWriter writer, Path filePath, String docsPath) throws TikaException {
        if (!Files.isRegularFile(filePath)) {
            return;
        }
        try {
            //Create lucene Document
            Document doc = new Document();

            // Getting Tika File
            Tika tika = new Tika();
            File file = new File(filePath.toString());

            // Extracting as a String
            String extractedData = tika.parseToString(file);

            // Lang detector
            OptimaizeLangDetector detector = new OptimaizeLangDetector();
            detector.loadModels();
            LanguageResult result = detector.detect(extractedData);

            Path fileName = filePath.getFileName();
            doc.add(new StringField("path", filePath.toString(), Store.YES));
            doc.add(new StringField("docsPath", docsPath, Store.YES));
            doc.add(new StringField("extractedData", extractedData, Store.NO));
            doc.add(new StringField("isPL", String.valueOf(result.isLanguage("pl")), Store.NO));
            doc.add(new StringField("fileName", fileName.toString(), Store.YES));
            log("Indexed: " + fileName.toString());
            String contents = "";
            if(getFileExtension(filePath.toString()).equals("pdf")){
                PDDocument PDFdoc = null;
                PDFdoc = PDDocument.load(file);
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setLineSeparator("\n");
                contents = stripper.getText(PDFdoc);
            }
            else{contents= new String(Files.readAllBytes(filePath));}



            doc.add(new TextField("contents", contents, Store.YES));

            String folder = filePath.toString();

            while (folder != null && !folder.equals(File.separatorChar)) {
                doc.add(new StringField("inFolder", folder, Store.YES));
                File f = new File(folder);
                folder = f.getParent();
            }

            try {
                writer.updateDocument(new Term("path", filePath.toString()), doc);
            } catch (IllegalArgumentException iae) {
                err("Text file too large");
            }
        } catch (IOException e) {
            err("IOException, Skipping file");
        }
    }

    public static String getFileExtension(String fullName) {
        checkNotNull(fullName);
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    protected static void deleteDocumentsCatalog(String docsPath) {
        IndexWriter writer = getIndexWriter();
        try {
            writer.deleteDocuments(new Term("docsPath", docsPath));
            writer.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void newFileIndex(String filePath) throws TikaException, IOException {
        String docsPath = findOriginalDir(filePath);

        addIndex(filePath, docsPath);


    }

    private static String findOriginalDir(String filePath) {
        String docsPath = null;
        String intersection;
        for (String x : crunchify.getCatalog()) {
            intersection = intersection(x, filePath);
            if (docsPath == null || intersection.length() > docsPath.length()) {
                docsPath = intersection;
            }
        }
        return docsPath;
    }

    private static String intersection(String x, String filePath) {
        if (x.charAt(0) != filePath.charAt(0)) return "";
        int end = min(x.length(), filePath.length());
        int i;
        for (i = 0; i <= end - 1; i++) {
            if (x.charAt(i) != filePath.charAt(i)) break;
        }
        return filePath.substring(0, i);
    }

    protected static void modifyFileIndex(String dirPath) throws IOException {
        deleteFileIndex(dirPath);
        try {
            newFileIndex(dirPath);
        } catch (TikaException e) {
            err("IOException, Skipping file");
        }
    }

    protected static void deleteFileIndex(String dirPath) throws IOException {
        IndexWriter writer = getIndexWriter();
        try {
            writer.deleteDocuments(new Term("inFolder", dirPath));
            writer.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void err(String string) {
        System.err.println("[ERROR] [INDEXER] " + string);
    }

    private static void log(String string) {
        System.out.println("[INDEXER] " + string);
    }
}