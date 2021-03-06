package pdfact.core.pipes.parse.stream.pdfbox.operators.graphic;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.Point;
import pdfact.core.pipes.parse.stream.pdfbox.operators.OperatorProcessor;

import java.io.IOException;
import java.util.List;

/**
 * m: Begins a new subpath.
 *
 * @author Claudius Korzen
 */
public class MoveTo extends OperatorProcessor {
    @Override
    public void process(Document pdf, Page page, Operator op, List<COSBase> args) throws IOException {
        COSNumber x = (COSNumber) args.get(0);
        COSNumber y = (COSNumber) args.get(1);

        Point point = new Point(x.floatValue(), y.floatValue());

        this.engine.transform(point);
        this.engine.getLinePath().moveTo(point.getX(), point.getY());
    }

    @Override
    public String getName() {
        return "m";
    }
}
