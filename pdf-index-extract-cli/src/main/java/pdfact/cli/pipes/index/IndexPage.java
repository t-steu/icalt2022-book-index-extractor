package pdfact.cli.pipes.index;

import pdfact.core.model.Page;
import pdfact.core.model.Rectangle;
import pdfact.core.model.TextBlock;
import pdfact.core.model.TextLine;
import pdfact.core.util.IndexEntry;
import pdfact.core.util.list.ElementList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pdfact.cli.pipes.index.IndexEntryTransformation.DASH_REGEX;

public class IndexPage {

    public static final String INDEX_HEADING_PATTERN = "[a-zA-Z]|(?:Numerals|Symbols|Index|[a-zA-Z]*[vV]erzeichnis)";

    private final float pageWidth;
    private final float pageHeight;
    private final double contentMaxY;
    private final double contentMinY;
    private final Page page;


    public IndexPage(Page p, int yMaxBoundary, int yMinBoundary) {

        this.page = p;
        this.pageWidth = p.getWidth();
        this.pageHeight = p.getHeight();


        double content_max_y = yMaxBoundary * 1.01;
        if (content_max_y < 0.8 * pageHeight) {
            content_max_y = 0.92 * pageHeight;
        }
        this.contentMaxY = content_max_y;


        double content_min_y = yMinBoundary * 0.99;
        if (content_min_y > 0.2 * pageHeight) {
            content_min_y = 0.08 * pageHeight;
        }
        this.contentMinY = content_min_y;
    }


    public List<IndexEntry> getPhrases() {
        List<IndexEntryTransformation> transformations = new ArrayList<>();

        List<TextLine> lineAccu = new ArrayList<>();

        float contentMinY = computeFooterYMax(page);

        List<TextLine> textLines = filterToIndexPhraseCandidateList(page.getTextLines(), contentMaxY, contentMinY);


        List<Rectangle> columnBoundingBoxes = computeColumnBoundingBoxes(textLines);


        for (TextLine line : textLines) {
            int currentColumn = assignColumn(line, columnBoundingBoxes);
            boolean isIndented = isIndented(line, columnBoundingBoxes.get(currentColumn));
            boolean startsWithDash = line.getText().strip().replaceAll(DASH_REGEX, "-").startsWith("-");

            if (isIndented || startsWithDash) {
                lineAccu.add(line);
            } else {
                if (!lineAccu.isEmpty())
                    transformations.add(new IndexEntryTransformation(lineAccu));
                lineAccu = new ArrayList<>();
                lineAccu.add(line);
            }


        }

        if (lineAccu.size() > 0) {
            transformations.add(new IndexEntryTransformation(lineAccu));
        }

        List<IndexEntry> entries = transformations.stream().flatMap(t -> t.apply().stream()).collect(Collectors.toList());

        fixAllEntriesEndWithNumber(entries);

        return entries;
    }

    /**
    If the last block has unusual width (too wide) and a larger than 2 line height vertical spacing, assume footer.
     */
    private float computeFooterYMax(Page page) {
        ElementList<TextBlock> blocks = page.getTextBlocks();
        Collections.sort(blocks, (b1, b2) -> Float.compare(b2.getPosition().getRectangle().getMinY(), b1.getPosition().getRectangle().getMinY()));

        if (blocks.size() < 2){
            return 0;
        }

        Rectangle boundingRect = blocks.getLastElement().getPosition().getRectangle();

        double meanTextlineHeight = page.getTextLines().stream()
                .mapToDouble(x -> x.getPosition().getRectangle().getHeight()).average().getAsDouble();

        List<Float> meanTextlineWidth = page.getTextLines().stream()
                .map(x -> x.getPosition().getRectangle().getWidth()).collect(Collectors.toList());

        double maxAllowedWidth = computeMaxAllowedWidthDeviation(meanTextlineWidth);

        Rectangle currentRect = blocks.get(blocks.size()-2).getPosition().getRectangle();
        float verticalSpacing = currentRect.getMaxY() - boundingRect.getMinY();

        if (verticalSpacing > 2 * meanTextlineHeight && maxAllowedWidth < boundingRect.getWidth()) {
            return boundingRect.getMaxY();
        }
        return 0;
    }

    private void fixAllEntriesEndWithNumber(List<IndexEntry> entries) {
        String numberEndingRegex = "([0-9-]+)$";

        double count = entries.stream().map(e -> e.getPhrase().matches(numberEndingRegex)).count();

        //if 50% of all entries on the page end with number something is off
        if (count / entries.size() > 0.5) {
            for (IndexEntry entry : entries) {
                for (IndexEntry subEntry : entry.getSubentries()) {
                    //updates entry in place
                    IndexEntryTransformation.parsePageNumbers(subEntry, subEntry.getPhrase(), numberEndingRegex);
                }
                //updates entry in place
                IndexEntryTransformation.parsePageNumbers(entry, entry.getPhrase(), numberEndingRegex);
            }
        }
    }


    private List<TextLine> filterToIndexPhraseCandidateList(List<TextLine> indexTextLines, double finalContent_max_y,
                                                            double finalContent_min_y) {

        List<TextLine> linesInYBoundaries = indexTextLines.stream().filter(line -> Math.round(line.getPosition().getRectangle().getMinY()) <= finalContent_max_y
                && Math.round(line.getPosition().getRectangle().getMaxY()) >= finalContent_min_y).collect(Collectors.toList());

        List<Float> meanTextlineWidth = linesInYBoundaries.stream()
                .map(x -> x.getPosition().getRectangle().getWidth()).collect(Collectors.toList());

        double maxAllowedWidth = computeMaxAllowedWidthDeviation(meanTextlineWidth);

        Stream<TextLine> onlyLinesWithSmallWidth = linesInYBoundaries.stream().filter(x -> x.getPosition().getRectangle().getWidth() < maxAllowedWidth);

        return onlyLinesWithSmallWidth
                .filter(line -> !line.getText().matches(INDEX_HEADING_PATTERN) && line.getBaseline() != null
                ).collect(Collectors.toList());
    }

    private static double computeMaxAllowedWidthDeviation(List<Float> list) {
        float avg = 0f;
        for (float i : list)
            avg += i;
        avg /= list.size();
        float var = 0f;
        for (float i : list)
            var += (i - avg) * (i - avg);
        var /= list.size();
        return Math.max(avg + Math.sqrt(var) * 3, avg + 15);
    }


    private boolean isIndented(TextLine line, Rectangle boundingBox) {
        Rectangle lineRect = line.getPosition().getRectangle();
        return (lineRect.getMinX() - boundingBox.getMinX()) > 0.02 * boundingBox.getWidth();
    }

    private int assignColumn(TextLine line, List<Rectangle> boundingBoxes) {
        int i = 0;
        Rectangle rect = line.getPosition().getRectangle();

        for (Rectangle r : boundingBoxes) {
            if (r.contains(rect))
                return i;
            i++;
        }
        return i;
    }

    private List<Rectangle> computeColumnBoundingBoxes(List<TextLine> indexTextLines) {

        List<Rectangle> nonHorizontalOverlappingRects = new ArrayList<>();

        for (TextLine line : indexTextLines) {

            Rectangle lineRect = line.getPosition().getRectangle();

            Rectangle overlapRect = overlapsHorizontallyWith(nonHorizontalOverlappingRects, lineRect);
            if (overlapRect == null) {
                nonHorizontalOverlappingRects.add(lineRect);
            } else {
                Rectangle unionRect = overlapRect.union(lineRect);
                int indexToReplace = nonHorizontalOverlappingRects.indexOf(overlapRect);
                nonHorizontalOverlappingRects.set(indexToReplace, unionRect);
            }
        }

        return nonHorizontalOverlappingRects;
    }


    private Rectangle overlapsHorizontallyWith(List<Rectangle> nonHorizontalOverlappingRects, Rectangle lineRect) {
        for (Rectangle r : nonHorizontalOverlappingRects) {
            if (lineRect.overlapsHorizontally(r)) {
                return r;
            }
        }
        return null;
    }


}
