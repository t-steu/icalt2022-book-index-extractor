package pdfact.core.pipes.parse.stream.pdfbox.operators.text;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.pipes.parse.stream.pdfbox.operators.OperatorProcessor;

import java.io.IOException;
import java.util.List;

/**
 * ET: End a text object, discarding the text matrix.
 *
 * @author Claudius Korzen.
 */
public class EndText extends OperatorProcessor {
    @Override
    public void process(Document pdf, Page page, Operator op, List<COSBase> args) throws IOException {
        this.engine.setTextMatrix(null);
        this.engine.setTextLineMatrix(null);
        // context.endText();
    }

    @Override
    public String getName() {
        return "ET";
    }
}