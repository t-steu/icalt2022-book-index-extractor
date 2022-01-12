package pdfact.core.pipes.semanticize.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pdfact.core.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A module that identifies the text blocks with the semantic role "body".
 *
 * @author Claudius Korzen
 */
public class BodyTextModule implements PdfTextSemanticizerModule {
    /**
     * The logger.
     */
    protected static Logger log = LogManager.getFormatterLogger("role-detection");

    @Override
    public void semanticize(Document pdf) {

        ArrayList<Integer> maxYRoundedPerPage = new ArrayList<>();
        ArrayList<Integer> minYRoundedPerPage = new ArrayList<>();

        ArrayList<Float> local_min_y = new ArrayList<>();
        ArrayList<Float> local_max_y = new ArrayList<>();

        maxYRoundedPerPage = new ArrayList<>();
        minYRoundedPerPage = new ArrayList<>();

        log.debug("=====================================================");
        log.debug("Detecting text blocks of semantic role '%s' ...", SemanticRole.BODY_TEXT);
        log.debug("=====================================================");

        if (pdf == null) {
            return;
        }

        List<Page> pages = pdf.getPages();

        if (pages == null) {
            return;
        }

        // Compute the most common font face in the PDF document.
        CharacterStatistic pdfCharStats = pdf.getCharacterStatistic();
        FontFace pdfFontFace = pdfCharStats.getMostCommonFontFace();

        for (Page page : pages) {
            if (page == null) {
                continue;
            }
            if (!local_max_y.isEmpty()) {
                maxYRoundedPerPage.add(Math.round(Collections.max(local_max_y)));
            }
            if (!local_min_y.isEmpty()) {
                minYRoundedPerPage.add(Math.round(Collections.min(local_min_y)));
            }
            local_max_y.removeAll(local_max_y);
            local_min_y.removeAll(local_min_y);

            List<String> ignore = new ArrayList<String>();
            ignore.add("Index");
            ignore.add("index");
            ignore.add("INDEX");

            for (int i = 0; i < 2000; i++) {
                ignore.add(Integer.toString(i));
            }

            OUTER_LOOP:
            for (TextBlock block : page.getTextBlocks()) {
                if (block == null) {
                    continue;
                }

                if (block.getSemanticRole() != null) {
                    continue;
                }

                // The text block is a member of the body text if its font face is
                // equal to the most common font face.
                CharacterStatistic blockCharStats = block.getCharacterStatistic();
                FontFace blockFontFace = blockCharStats.getMostCommonFontFace();

                Font pdfFont = pdfFontFace.getFont();
                Font blockFont = blockFontFace.getFont();
                if (pdfFont != blockFont) {
                    continue;
                }

                float pdfFontSize = pdfFontFace.getFontSize();
                float blockFontSize = blockFontFace.getFontSize();
                if (Math.abs(pdfFontSize - blockFontSize) > 0.05 * pdfFontSize) {
                    continue;
                }

                for (int i = 0; i < ignore.size(); i++) {
                    if (block.getText().equals(ignore.get(i))) {
                        continue OUTER_LOOP;
                    }
                }

                log.debug("-----------------------------------------------------");
                log.debug("Text block: \"%s\" ...", block.getText());
                log.debug("... page:          %d", block.getPosition().getPageNumber());
                log.debug("... font face:     %s", block.getCharacterStatistic().getMostCommonFontFace());
                log.debug("... assigned role: %s", SemanticRole.BODY_TEXT);
                log.debug("... role reason:   the block exhibits the most common font face");

                block.setSemanticRole(SemanticRole.BODY_TEXT);
                local_max_y.add(block.getPosition().getRectangle().getMaxY());
                local_min_y.add(block.getPosition().getRectangle().getMinY());

            }
        }

        pdf.setRoundMinYForPages(minYRoundedPerPage);
        pdf.setRoundMaxYForPages(maxYRoundedPerPage);

    }

}
