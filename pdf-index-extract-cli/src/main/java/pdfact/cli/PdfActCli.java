package pdfact.cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.logging.log4j.LogManager;
import pdfact.cli.model.ExtractionUnit;
import pdfact.cli.model.SerializationFormat;
import pdfact.cli.util.exception.PdfActParseCommandLineException;
import pdfact.core.model.SemanticRole;
import pdfact.core.util.exception.PdfActException;

import java.nio.file.Paths;
import java.util.*;

/**
 * The command line interface of PdfAct.
 *
 * @author Claudius Korzen
 */
public class PdfActCli {
    /**
     * Starts this command line interface.
     *
     * @param args The command line arguments.
     */
    protected void start(String[] args) {
        int statusCode = 0;
        String errorMessage = null;
        Throwable cause = null;

        // Create the command line argument parser.
        PdfActCommandLineParser parser = new PdfActCommandLineParser();

        try {
            // Parse the command line arguments.
            parser.parseArgs(args);

            // Create an instance of PdfAct.
            PdfAct pdfAct = new PdfAct();

            // Pass the debugging flags.
            pdfAct.setDebugPdfParsing(parser.isDebugPdfParsing);
            pdfAct.setDebugCharacterExtraction(parser.isDebugCharExtraction);
            pdfAct.setDebugSplittingLigatures(parser.isDebugSplittingLigatures);
            pdfAct.setDebugMergingDiacritics(parser.isDebugMergingDiacritics);
            pdfAct.setDebugTextLineDetection(parser.isDebugLineDetection);
            pdfAct.setDebugWordDetection(parser.isDebugWordDetection);
            pdfAct.setDebugTextBlockDetection(parser.isDebugBlockDetection);
            pdfAct.setDebugRoleDetection(parser.isDebugRoleDetection);
            pdfAct.setDebugParagraphDetection(parser.isDebugParagraphDetection);
            pdfAct.setDebugWordDehyphenation(parser.isDebugWordDehyphenation);

            // Pass the serialization format if there is any.
            String serializationFormatStr = parser.serializationFormat;
            if (serializationFormatStr != null) {
                pdfAct.setSerializationFormat(SerializationFormat.fromString(serializationFormatStr));
            }

            // Pass the serialization target path.
            String serializationPathStr = parser.serializationPath;
            if (serializationPathStr != null) {
                pdfAct.setSerializationPath(Paths.get(serializationPathStr));
            } else {
                pdfAct.setSerializationStream(System.out);
            }

            // Pass the target of the visualization.
            String visualizationPathStr = parser.visualizationPath;
            if (visualizationPathStr != null) {
                pdfAct.setVisualizationPath(Paths.get(visualizationPathStr));
            }

            // Pass the chosen text unit.
            List<String> extractionUnits = parser.extractionUnits;
            if (extractionUnits != null) {
                pdfAct.setExtractionUnits(ExtractionUnit.fromStrings(extractionUnits));
            }

            // Compute the semantic roles to include on serialization & visualization.
            Set<String> roles = new HashSet<>();
            List<String> semanticRolesToInclude = parser.semanticRolesToInclude;
            if (semanticRolesToInclude != null) {
                roles.addAll(semanticRolesToInclude);
            }
            List<String> semanticRolesToExclude = parser.semanticRolesToExclude;
            if (semanticRolesToExclude != null) {
                roles.removeAll(semanticRolesToExclude);
            }
            pdfAct.setSemanticRoles(SemanticRole.fromStrings(roles));

            // Set the "with control characters"-flag.
            pdfAct.setInsertControlCharacters(parser.withControlCharacters);

            // Pass the target of the index extraction.
            String indexExtractionPathStr = parser.indexExtractionPath;
            if (indexExtractionPathStr != null) {
                pdfAct.setIndexExtractionPath(Paths.get(indexExtractionPathStr));
            }
            // Run PdfAct.
            pdfAct.parse(parser.pdfPath);
        } catch (PdfActException e) {
            statusCode = e.getExitCode();
            errorMessage = e.getMessage();
            cause = e.getCause();
        }

        if (statusCode != 0) {
            // Print the error message (regardless of the log level).
            System.err.println(errorMessage);
            // Print the stack trace if there is any and debugging is enabled.
            if (cause != null && LogManager.getRootLogger().isDebugEnabled()) {
                cause.printStackTrace();
            }
        }

        // System.exit(statusCode);
    }

    // ==============================================================================================

    /**
     * The main method to run the command line interface.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        new PdfActCli().start(args);
    }

    // ==============================================================================================

    /**
     * A parser to parse the command line arguments.
     *
     * @author Claudius Korzen
     */
    static class PdfActCommandLineParser {
        /**
         * The command line argument parser.
         */
        protected ArgumentParser parser;

        // ============================================================================================

        /**
         * The name of the option to define the path to the PDF file to process.
         */
        public static final String PDF_PATH = "pdfPath";

        /**
         * The path to the PDF file to process.
         */
        @Arg(dest = PDF_PATH)
        public String pdfPath;

        // ============================================================================================

        /**
         * The name of the option to define the target path for the serialization.
         */
        public static final String SERIALIZE_PATH = "serializationPath";

        /**
         * The target path for the serialization.
         */
        @Arg(dest = SERIALIZE_PATH)
        public String serializationPath;

        // ============================================================================================

        /**
         * The name of the option to define the serialization format.
         */
        public static final String SERIALIZE_FORMAT = "format";

        /**
         * The serialization format.
         */
        @Arg(dest = SERIALIZE_FORMAT)
        public String serializationFormat = "txt";

        // ============================================================================================

        /**
         * The name of the option to define the target path for the visualization.
         */
        public static final String VISUALIZATION_PATH = "visualize";

        /**
         * The target path for the visualization.
         */
        @Arg(dest = VISUALIZATION_PATH)
        public String visualizationPath;

        // ============================================================================================

        /**
         * The name of the option to define the units to extract.
         */
        public static final String EXTRACTION_UNITS = "units";

        /**
         * The text unit to extract.
         */
        @Arg(dest = EXTRACTION_UNITS)
        public List<String> extractionUnits = Arrays.asList("paragraphs");

        // ============================================================================================

        /**
         * The name of the option to define the semantic roles to include (text blocks
         * with a semantic role that is not included won't be extracted).
         */
        public static final String INCLUDE_SEMANTIC_ROLES = "include-roles";

        /**
         * The semantic role(s) to include (text blocks with a semantic role that is not
         * included won't be extracted).
         */
        @Arg(dest = INCLUDE_SEMANTIC_ROLES)
        public List<String> semanticRolesToInclude = new ArrayList<>(SemanticRole.getNames());

        // ============================================================================================

        /**
         * The name of the option to define the semantic roles to exclude (text blocks
         * with a semantic role that is excluded won't be extracted).
         */
        public static final String EXCLUDE_SEMANTIC_ROLES = "exclude-roles";

        /**
         * The semantic role(s) to exclude (text blocks with a semantic role that is
         * excluded won't be extracted).
         */
        @Arg(dest = EXCLUDE_SEMANTIC_ROLES)
        public List<String> semanticRolesToExclude = new ArrayList<>();

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the PDF
         * parsing step.
         */
        public static final String DEBUG_PDF_PARSING = "debug-pdf-parsing";

        /**
         * The boolean flag indicating whether or not to print debug info about the PDF
         * parsing step.
         */
        @Arg(dest = DEBUG_PDF_PARSING)
        public boolean isDebugPdfParsing = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the
         * extracted characters.
         */
        public static final String DEBUG_CHAR_EXTRACTION = "debug-character-extraction";

        /**
         * The boolean flag indicating whether or not to print debug info about the
         * extracted chars.
         */
        @Arg(dest = DEBUG_CHAR_EXTRACTION)
        public boolean isDebugCharExtraction = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about splitting
         * ligatures.
         */
        public static final String DEBUG_SPLITTING_LIGATURES = "debug-splitting-ligatures";

        /**
         * The boolean flag indicating whether or not to print debug info about
         * splitting ligatures.
         */
        @Arg(dest = DEBUG_SPLITTING_LIGATURES)
        public boolean isDebugSplittingLigatures = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about merging
         * diacritics.
         */
        public static final String DEBUG_MERGING_DIACRITICS = "debug-merging-diacritics";

        /**
         * The boolean flag indicating whether or not to print debug info about
         * splitting ligatures.
         */
        @Arg(dest = DEBUG_MERGING_DIACRITICS)
        public boolean isDebugMergingDiacritics = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the text
         * line detection.
         */
        public static final String DEBUG_LINE_DETECTION = "debug-text-line-detection";

        /**
         * The boolean flag indicating whether or not to print debug info about the text
         * line detection.
         */
        @Arg(dest = DEBUG_LINE_DETECTION)
        public boolean isDebugLineDetection = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the word
         * detection.
         */
        public static final String DEBUG_WORD_DETECTION = "debug-word-detection";

        /**
         * The boolean flag indicating whether or not to print debug info about the word
         * detection.
         */
        @Arg(dest = DEBUG_WORD_DETECTION)
        public boolean isDebugWordDetection = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the text
         * block detection.
         */
        public static final String DEBUG_BLOCK_DETECTION = "debug-text-block-detection";

        /**
         * The boolean flag indicating whether or not to print debug info about the text
         * block detection.
         */
        @Arg(dest = DEBUG_BLOCK_DETECTION)
        public boolean isDebugBlockDetection = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the
         * semantic roles detection.
         */
        public static final String DEBUG_ROLE_DETECTION = "debug-semantic-role-detection";

        /**
         * The boolean flag indicating whether or not to print debug info about the
         * semantic roles detection.
         */
        @Arg(dest = DEBUG_ROLE_DETECTION)
        public boolean isDebugRoleDetection = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the
         * paragraphs detection.
         */
        public static final String DEBUG_PARAGRAPH_DETECTION = "debug-paragraph-detection";

        /**
         * The boolean flag indicating whether or not to print debug info about the
         * paragraphs detection.
         */
        @Arg(dest = DEBUG_PARAGRAPH_DETECTION)
        public boolean isDebugParagraphDetection = false;

        // ============================================================================================

        /**
         * The name of the option to enable the printing of debug info about the word
         * dehyphenation.
         */
        public static final String DEBUG_WORD_DEHYPHENATION = "debug-word-dehyphenation";

        /**
         * The boolean flag indicating whether or not to print debug info about the word
         * dehyphenation.
         */
        @Arg(dest = DEBUG_WORD_DEHYPHENATION)
        public boolean isDebugWordDehyphenation = false;

        // ============================================================================================

        /**
         * The name of the option to define the "with control characters" flag.
         */
        public static final String WITH_CONTROL_CHARACTERS = "with-control-characters";

        /**
         * The flag indicating whether or not to add control characters to the TXT
         * serialization output.
         */
        @Arg(dest = WITH_CONTROL_CHARACTERS)
        public boolean withControlCharacters = false;

        /**
         * The name of the option to enable the extraction of the index.
         */
        public static final String INDEX_EXTRACTION = "index-xml-output-path";

        /**
         * The flag indicating whether or not to extract the index
         */
        @Arg(dest = INDEX_EXTRACTION)
        public String indexExtractionPath;

        // ============================================================================================

        /**
         * Creates a new command line argument parser.
         */
        public PdfActCommandLineParser() {
            this.parser = ArgumentParsers.newFor("pdfIndexExtract").terminalWidthDetection(false).defaultFormatWidth(100)
                    .build();
            this.parser.description("A tool to extract the text, structure and layout from PDF files.");

            // Add an option to define the path to the PDF file to be processed.
            this.parser.addArgument(PDF_PATH).dest(PDF_PATH).required(true).metavar("<pdf-input-path>")
                    .help("The path to the PDF file to be processed.");

            // Add an option to enable the extratcion of the index.
            this.parser.addArgument(INDEX_EXTRACTION).dest(INDEX_EXTRACTION).required(true).type(String.class)
                    .metavar("<index-xml-output-path>")
                    .help("The option to extract the index from the pdf. The extracted index will be written to a xml file");
        }

        /**
         * Parses the given command line arguments.
         *
         * @param args The command line arguments to parse.
         * @throws PdfActException If parsing the command line arguments fails.
         */
        public void parseArgs(String[] args) throws PdfActException {
            try {
                this.parser.parseArgs(args, this);
            } catch (HelpScreenException e) {
                // Set the status code to 0, such that no error message is shown.
                throw new PdfActParseCommandLineException(null, 0, e);
            } catch (ArgumentParserException e) {
                String message = e.getMessage() + "\n\n" + getUsage();
                throw new PdfActParseCommandLineException(message, e);
            }
        }

        /**
         * Returns the usage for this command line parser.
         *
         * @return The usage for this command line parser.
         */
        public String getUsage() {
            return this.parser.formatUsage();
        }

        /**
         * Returns the help for this command line parser.
         *
         * @return The help for this command line parser.
         */
        public String getHelp() {
            return this.parser.formatHelp();
        }
    }

    // ==============================================================================================

    /**
     * Argument action to split a given string at a given delimiter and to store a
     * list of all resulting substrings.
     */
    private static class SplitAtDelimiterAction implements ArgumentAction {
        /**
         * The delimiter to split at.
         */
        protected String delimiter;

        @Override
        public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value)
                throws ArgumentParserException {
            if (value != null) {
                attrs.put(arg.getDest(), Arrays.asList(((String) value).split(delimiter)));
            }
        }

        @Override
        public void onAttach(Argument arg) {
        }

        @Override
        public boolean consumeArgument() {
            return true;
        }
    }
}
