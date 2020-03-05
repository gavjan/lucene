package pl.edu.mimuw.gc401929.core;

import java.io.IOException;

import org.jline.builtins.Completers;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JLineClass {
    private static String lang = "en";
    private static Boolean details = false;
    private static final int TERM = 1;
    private static final int PHRASE = 2;
    private static final int FUZZY = 3;
    private static int limit = Integer.MAX_VALUE;
    private static boolean color = false;
    private static int mode = TERM;
    private static Logger logger = LoggerFactory.getLogger(JLineClass.class);

    public static void main(String[] args) throws Exception {
        logger.info("Warming up...");
        try (Terminal terminal = TerminalBuilder.builder().dumb(true).jna(false).jansi(true).build()) {
            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new Completers.FileNameCompleter())
                    .build();

            terminal.writer().println("Ready to take commands...");

            while (1 < 2) {
                String line = null;
                try {
                    line = lineReader.readLine("> ");
                    if (line == null || line.equals("") || line.charAt(0) == ' ') {
                        continue;

                    } else if (line.charAt(0) == '%') {
                        takeArgument(line);
                    } else if (!Character.isLetter(line.charAt(0))
                            && !Character.isDigit(line.charAt(0))) {
                        continue;
                    } else if (details) {
                        CustomSearch.highlight(line, limit, mode, color);
                    } else {
                        CustomSearch.search(line, limit, mode);
                    }

                } catch (UserInterruptException | EndOfFileException e) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("An error has occurred", e);
        }

    }

    private static void takeArgument(String line) {
        if (line.startsWith("%lang ")) lang = line.substring("%lang ".length());
        if (line.startsWith("%details ")) details = line.substring("%details ".length()).equals("on");
        if (line.startsWith("%limit ")) limit = Integer.parseInt(line.substring("%limit ".length()));
        if (limit == 0) limit = Integer.MAX_VALUE;
        if (line.startsWith("%color ")) color = line.substring("%color ".length()).equals("on");
        if (line.equals("%term")) mode = TERM;
        if (line.equals("%phrase")) mode = PHRASE;
        if (line.equals("%fuzzy")) mode = FUZZY;
    }

}
