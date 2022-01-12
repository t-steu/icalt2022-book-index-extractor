package pdfact.cli.pipes.index;

import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.TextLine;

import java.util.ArrayList;
import java.util.List;

public class HeuristicBasedIndexPageSearch implements IndexPageSearch {

    private int firstPageNumber = Integer.MAX_VALUE;

    public List<Page> extractIndexPages(Document pdf) {
        int amountOfPossibleIndexLines = 0;
        int amountOfLines = 0;
        boolean sectionFound = false;
        int totalNumberOfPages = pdf.getLastPage().getPageNumber();
        List<Page> indexPages = new ArrayList<>();

        for (Page p : pdf.getPages()) {
            // Assume index is part of the last 20% of the pdf -> avoid finding table of
            // contents as index-section
            // Also we need to do the assumption here that more then 20 lines exists to
            // avoid some special pages
            if (p.getPageNumber() < totalNumberOfPages * 0.8 || p.getTextLines().size() < 20) {
                amountOfLines = 0;
                amountOfPossibleIndexLines = 0;
                continue;
            }
            if (sectionFound)
                indexPages.add(p);
            else {
                amountOfLines = (p.getTextLines().size());
                boolean containsLiteral = false;
                for (TextLine line : p.getTextLines()) {
                    String text = line.getText();
                    containsLiteral |= text.strip().matches("[A-Z-a-z]");
                    // Last two digits because the last will often be a ","

                    boolean entryHasIndexStructure = text.matches("[a-zA-Z-; ]+,?(\\s+|(\\s?\\.\\s?)+)([0-9]+,? ?)+");
                    if (entryHasIndexStructure)
                        amountOfPossibleIndexLines++;
                }
                if (amountOfLines != 0 && ((double) amountOfPossibleIndexLines / amountOfLines) > 0.7 && containsLiteral) {
                    sectionFound = true;
                    indexPages.add(p);
                }
                amountOfLines = 0;
                amountOfPossibleIndexLines = 0;
            }
        }
        return indexPages;
    }

}
