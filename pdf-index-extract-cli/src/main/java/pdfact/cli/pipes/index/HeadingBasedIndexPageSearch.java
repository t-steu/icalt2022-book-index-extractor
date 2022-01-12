package pdfact.cli.pipes.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.SemanticRole;
import pdfact.core.model.TextBlock;
import pdfact.core.util.list.ElementList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class HeadingBasedIndexPageSearch implements IndexPageSearch {

    private static final Logger log = LogManager.getLogger(IndexExtraction.class);

    private static final Pattern INDEX_HEADING_PATTERN = Pattern
            .compile("(?i)(index|verzeichnis|namenverzeichnis|sachverzeichnis|"
                    + "namen- und sachverzeichnis|stichwortverzeichnis|register|stichwortregister|konkordanz|"
                    + "sachwortverzeichnis|sach- und personenregister|Sach- und Namenverzeichnis|sachregister)");

    private final boolean heuristicHeadingSearch;

    private int firstIndexPageNumber;

    public HeadingBasedIndexPageSearch(boolean heuristicHeadingSearch) {
        this.heuristicHeadingSearch = heuristicHeadingSearch;
    }

    public List<Page> extractIndexPages(Document pdf) {

        if (pdf == null) {
            return Collections.emptyList();
        }

        firstIndexPageNumber = Integer.MAX_VALUE;

        boolean isIndexSection = false;
        List<Page> pages = pdf.getPages();
        ArrayList<Page> indexPages = new ArrayList<>();

        // assume that a index will occur near the end of a pdf around last 30 % of it
        int firstPossibleIndexPageNumber = (int) (pdf.getLastPage().getPageNumber() * 0.7);

        if (pages == null) {
            return Collections.emptyList();
        }

        for (Page page : pages) {
            if (page == null) {
                continue;
            }
            int currentPageNumber = page.getPageNumber();
            ElementList<TextBlock> blocks = page.getTextBlocks();
            Collections.sort(blocks, (b1, b2) -> Float.compare(b2.getPosition().getRectangle().getMinY(), b1.getPosition().getRectangle().getMinY()));

            for (TextBlock block : blocks) {
                if (block == null) {
                    continue;
                }

                int currentBlockPageNumber = block.getPosition().getPageNumber();

                if (currentBlockPageNumber <= firstPossibleIndexPageNumber) {
                    continue;
                }

                // add all lines of text which are relevant for the index extraction
                if (isIndexSection) {
                    if (firstIndexPageNumber > currentPageNumber) {
                        firstIndexPageNumber = currentPageNumber;
                    }
                    indexPages.add(page);
                    break;
                } else if (!heuristicHeadingSearch && hasIndexHeadingBySemanticRole(block)) {
                    log.info("INDEX FOUND ON PAGE " + currentBlockPageNumber);
                    isIndexSection = true;
                    continue;
                } else if (hasIndexHeadingByHeuristicFontDifference(block, pdf)) {
                    log.info("INDEX FOUND ON PAGE " + currentBlockPageNumber);
                    isIndexSection = true;
                    continue;
                }

            }
        }

        return indexPages;
    }

    private boolean hasIndexHeadingBySemanticRole(TextBlock block) {
        // Check for a heading with text index
        String text = block.getText();
        return INDEX_HEADING_PATTERN.matcher(text.toLowerCase()).matches()
                && block.getSemanticRole() == SemanticRole.HEADING && block.getSemanticRole() != SemanticRole.BODY_TEXT;
    }

    private boolean hasIndexHeadingByHeuristicFontDifference(TextBlock block, Document pdf) {
        // Check if block is heading with text index and if the fontsize is bigger than
        // the average Fontsize
        String text = block.getText();
        return INDEX_HEADING_PATTERN.matcher(text.toLowerCase()).matches() && block.getCharacterStatistic()
                .getAverageFontsize() > pdf.getCharacterStatistic().getAverageFontsize();
    }


}
