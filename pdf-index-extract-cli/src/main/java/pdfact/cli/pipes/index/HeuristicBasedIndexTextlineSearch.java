package pdfact.cli.pipes.index;

import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.TextLine;

import java.util.ArrayList;
import java.util.List;

public class HeuristicBasedIndexTextlineSearch implements IndexTextlineSearch {

    private int firstPageNumber = Integer.MAX_VALUE;

    @Override
    public List<TextLine> extractIndexTextLines(Document pdf) {
        int amountOfPossibleIndexLines = 0;
        int amountOfLines = 0;
        boolean sectionFound = false;
        int totalNumberOfPages = pdf.getLastPage().getPageNumber();
        List<TextLine> indexTextLines = new ArrayList<>();

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
                indexTextLines.addAll(p.getTextLines());
            else {
                amountOfLines = (p.getTextLines().size());
                for (TextLine line : p.getTextLines()) {
                    String text = line.getText();
                    // Last two digits because the last will often be a ","
                    int textLength = text.length();
                    if (textLength > 1 && (Character.isDigit(text.charAt(textLength - 1))))
                        amountOfPossibleIndexLines++;
                }
                if (amountOfLines != 0 && ((double) amountOfPossibleIndexLines / amountOfLines) > 0.7) {
                    sectionFound = true;
                    firstPageNumber = p.getPageNumber();
                    indexTextLines.addAll(p.getTextLines());
                }
                amountOfLines = 0;
                amountOfPossibleIndexLines = 0;
            }
        }
        return indexTextLines;
    }

    @Override
    public int getFirstPageNumber() {
        return firstPageNumber;
    }

}
