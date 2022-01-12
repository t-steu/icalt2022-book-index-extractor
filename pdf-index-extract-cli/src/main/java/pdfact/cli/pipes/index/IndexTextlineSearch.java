package pdfact.cli.pipes.index;

import pdfact.core.model.Document;
import pdfact.core.model.TextLine;

import java.util.List;

public interface IndexTextlineSearch {

    List<TextLine> extractIndexTextLines(Document pdf);

    int getFirstPageNumber();
}
