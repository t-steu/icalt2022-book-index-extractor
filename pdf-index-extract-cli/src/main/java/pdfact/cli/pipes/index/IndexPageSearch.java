package pdfact.cli.pipes.index;

import pdfact.core.model.Document;
import pdfact.core.model.Page;

import java.util.List;

public interface IndexPageSearch {

    List<Page> extractIndexPages(Document pdf);
}
