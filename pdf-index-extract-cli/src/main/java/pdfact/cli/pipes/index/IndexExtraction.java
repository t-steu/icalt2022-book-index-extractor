package pdfact.cli.pipes.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.TextLine;
import pdfact.core.util.IndexEntry;
import pdfact.core.util.WriteToXML;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Extraction of the index from a pdf
 *
 * @author Ercan Akar, Julian Barthel
 */
public class IndexExtraction {

    private static final Logger log = LogManager.getLogger(IndexExtraction.class);


    public void extract(Document pdf, Path path) {

        // -------- search index lines in PDF ------------
        List<IndexPageSearch> availableSearches = Arrays.asList(
                new HeadingBasedIndexPageSearch(false),
                new HeadingBasedIndexPageSearch(true),
                new HeuristicBasedIndexPageSearch());

        List<Page> indexPages = Collections.emptyList();

        for (IndexPageSearch search : availableSearches) {
            indexPages = search.extractIndexPages(pdf);
            if (!indexPages.isEmpty()) {
                break;
            }
        }

        if (indexPages.isEmpty()) {
            writeIndexToXML(Collections.emptyList(), pdf, path);
            return;
        }


        int yMaxBoundary = getMostRepeated(pdf.getMaxYRoundedPerPage());
        int yMinBoundary = getMostRepeated(pdf.getMinYRoundedPerPage());



        List<IndexEntry> result = new ArrayList<>();
        for (Page page : indexPages) {
            IndexPage indexPage = new IndexPage(page, yMaxBoundary, yMinBoundary);
            List<IndexEntry> phrases = indexPage.getPhrases();
            result.addAll(phrases);
        }

        attachOffsets(pdf, result);

        writeIndexToXML(result, pdf, path);
    }


    public void attachOffsets(Document pdf, List<IndexEntry> entries) {
        for (IndexEntry entry : entries) {
            List<Integer> offsets = entry.getNumbers().stream().map(this::toIntegerPage).map(x -> computeOffsetForEntry(pdf, x)).collect(Collectors.toList());
            entry.addOffsets(offsets);
            attachOffsets(pdf, entry.getSubentries());
        }

    }


    // get page number from a digit in indexTextLines
    // also limit the position of that digit it must be outside of the content
    // y-coordinates
    // Check only after the begin of the index
    // ignore Doi Links and Authors
    private int computeBookPageNumber(List<TextLine> indexTextLines, float pageHeight) {

        if (indexTextLines.isEmpty()) {
            return -1;
        }
        int pagenumber = indexTextLines.get(0).getPosition().getPageNumber();

        // page numbers must be either top 91% or bottom 9%.
        // we dont need to calculate it dynamically.
        // it worked all pdfs I checked incl. Bosch2009_Book_Algebra
        // Arbeitsmarktökonomik__711d3a13-e970-4a3a-a3c2-468bfdcaaa18
        // etc..
        float page_number_top = pageHeight * 0.91f;
        float page_number_bottom = pageHeight * 0.09f;

        int relativePageNumber = 0;
        Pattern actualPageNumberPattern = Pattern.compile("[0-9]+[^,]$|^[^–-][0-9]+");

        for (TextLine line : indexTextLines) {
            Matcher m = actualPageNumberPattern.matcher(line.getText());
            Pattern forbiddenWords = Pattern.compile("(?i)doi|Author|Springer|©|[.]|[,]");
            boolean containsRelativePageNumber = m.find()
                    && (line.getPosition().getRectangle().getMaxY() < page_number_bottom
                    || line.getPosition().getRectangle().getMinY() > page_number_top)
                    && line.getPosition().getPageNumber() >= pagenumber
                    && !forbiddenWords.matcher(line.getText()).find();

            if (containsRelativePageNumber) {

                try {
                    relativePageNumber = Integer.parseInt(m.group(0));
                    return relativePageNumber;
                } catch (NumberFormatException ignored) {

                }

                break;
            }
        }
        return -1;
    }


    private int toIntegerPage(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            if (s.matches("[0-9]+-[0-9]")) {
                return toIntegerPage(s.split("-")[0]);
            }
        }
        return -1;
    }

    private int computeOffsetForEntry(Document pdf, int entryBookPage) {
        int pageBookPageNumber = -1;
        int offset = -1;

        if (entryBookPage == -1)
            return 0;

        while (pageBookPageNumber <= entryBookPage && (entryBookPage + offset + 1) < pdf.getPages().size()) {
            offset += 1;
            var page = pdf.getPages().get(entryBookPage + offset);
            pageBookPageNumber = computeBookPageNumber(page.getTextLines(), page.getHeight());
        }

        if ((entryBookPage + offset + 1) >= pdf.getPages().size()) {
            return 0;
        }
        return offset;
    }


    /**
     * The method will write all given index entries {@link IndexEntry} to an xml
     * file See {@link WriteToXML} for more details about the writing
     *
     * @param entries a {@link ArrayList} with {@link IndexEntry}
     * @param pdf     the pdf document
     * @param path    the path to write
     */
    public void writeIndexToXML(List<IndexEntry> entries, Document pdf, Path path) {
        WriteToXML xmlWriter = new WriteToXML();
        String pdfName = pdf.getFile().getName();
        xmlWriter.saveToXML(path, pdfName, entries);
    }


    private int getMostRepeated(ArrayList<Integer> arr) {
        Collections.sort(arr);

        return arr.stream().reduce(BinaryOperator.maxBy(Comparator.comparingInt(o -> Collections.frequency(arr, o))))
                .orElse(0);
    }

}
