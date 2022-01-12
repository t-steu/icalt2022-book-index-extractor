package pdfact.cli.pipes.index;

import pdfact.core.model.Document;

import java.nio.file.Path;

public class PlainIndexExtractionPipe implements IndexExtractionPipe {

    protected Path indexExtractionPath;

    public PlainIndexExtractionPipe(Path indexExtractionPath) {
        this.indexExtractionPath = indexExtractionPath;
    }

    public void setIndexExtractionPath(Path indexExtractionPath) {
        this.indexExtractionPath = indexExtractionPath;
    }

    public Path getIndexExtractionPath() {
        return this.indexExtractionPath;
    }

    @Override
    public Document execute(Document pdf) {
        detectIndex(pdf);
        return pdf;
    }

    @Override
    public Document detectIndex(Document pdf) {
        new IndexExtraction().extract(pdf, indexExtractionPath);
        return pdf;
    }
}
