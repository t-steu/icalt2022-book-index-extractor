package pdfact.cli.pipes.index;

import pdfact.core.model.Document;
import pdfact.core.util.exception.PdfActException;
import pdfact.core.util.pipeline.Pipe;

public interface IndexExtractionPipe extends Pipe {

    Document execute(Document pdf) throws PdfActException;

    Document detectIndex(Document pdf);
}
