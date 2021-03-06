package pdfact.core.pipes.semanticize.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.SemanticRole;
import pdfact.core.model.TextBlock;

import java.util.List;

/**
 * A module that identifies the text blocks with the semantic role "appendix".
 *
 * @author Claudius Korzen
 */
public class AppendixModule implements PdfTextSemanticizerModule {
    /**
     * The logger.
     */
    protected static Logger log = LogManager.getFormatterLogger("role-detection");

    /**
     * A boolean flag that indicates whether the current text block is a member of
     * the appendix or not.
     */
    protected boolean isAppendix = false;

    @Override
    public void semanticize(Document pdf) {
        log.debug("=====================================================");
        log.debug("Detecting text blocks of semantic role '%s' ...", SemanticRole.APPENDIX);
        log.debug("=====================================================");

        if (pdf == null) {
            return;
        }

        List<Page> pages = pdf.getPages();
        if (pages == null) {
            return;
        }

        for (Page page : pages) {
            if (page == null) {
                continue;
            }

            for (TextBlock block : page.getTextBlocks()) {
                if (block == null) {
                    continue;
                }

                SemanticRole role = block.getSemanticRole();
                SemanticRole secondaryRole = block.getSecondarySemanticRole();

                // Check if the current block is a section heading (which would
                // denote the end of the appendix).
                if (this.isAppendix && role == SemanticRole.HEADING) {
                    this.isAppendix = false;
                }

                if (this.isAppendix) {
                    log.debug("-----------------------------------------------------");
                    log.debug("Text block: \"%s\" ...", block.getText());
                    log.debug("... page:          %d", block.getPosition().getPageNumber());
                    log.debug("... assigned role: %s", SemanticRole.APPENDIX);
                    log.debug("... role reason:   the block is located between the detected "
                            + "start/end of an Appendix section");
                    block.setSemanticRole(SemanticRole.APPENDIX);
                }

                // Check if the current block is the heading of the appendix (which
                // would denote the start of the appendix).
                if (role == SemanticRole.HEADING && secondaryRole == SemanticRole.APPENDIX) {
                    this.isAppendix = true;
                }
            }
        }
    }
}
