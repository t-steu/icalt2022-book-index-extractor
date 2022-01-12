package pdfact.core.util.xycut;

import pdfact.core.model.Character;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.Rectangle;
import pdfact.core.util.comparator.MaxYComparator;
import pdfact.core.util.comparator.MinXComparator;
import pdfact.core.util.list.ElementList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class that cuts a list of characters horizontally and vertically into
 * (smaller) blocks.
 *
 * @author Claudius Korzen
 */
public abstract class XYCut {
    /**
     * Cuts the given characters into blocks of type T.
     *
     * @param pdf        The PDF document to which the characters belong to.
     * @param page       The page in which the characters are located.
     * @param characters The characters to cut.
     * @return The list of resulting blocks.
     */
    public List<ElementList<Character>> cut(Document pdf, Page page, ElementList<Character> characters) {
        List<ElementList<Character>> target = new ArrayList<>();
        cut(pdf, page, characters, target);
//		target = cutFast(pdf, page, characters);
        return target;
    }

    /**
     * Cuts the given characters into blocks of type T and adds them to the given
     * result list.
     *
     * @param pdf    The PDF document to which the characters belong to.
     * @param page   The page in which the characters are located.
     * @param origin The characters to be cut.
     * @param target The list of blocks to fill.
     */
    protected void cut(Document pdf, Page page, ElementList<Character> origin, List<ElementList<Character>> target) {
        // Cut the characters vertically (x-cut).
        List<ElementList<Character>> xBlocks = xCut(pdf, page, origin);

        for (ElementList<Character> xBlock : xBlocks) {
            // Cut the characters horizontally (y-cut).
            List<ElementList<Character>> yBlocks = yCut(pdf, page, xBlock);
            if (xBlocks.size() == 1 && yBlocks.size() == 1) {
                // Both cuts results in a single blocks. So, the characters could *not*
                // be cut. Pack them and add them to the result list.
                ElementList<Character> block = yBlocks.get(0);
                if (block != null && !block.isEmpty()) {
                    target.add(block);
                }
            } else {
                // The characters could be cut. Cut the resulted blocks recursively.
                for (ElementList<Character> yBlock : yBlocks) {
                    cut(pdf, page, yBlock, target);
                }
            }
        }
    }

    /**
     * Takes a list of characters and iterates them by sweeping a lane in x
     * direction in order to find a position to cut the characters vertically into a
     * left half and a right half.
     *
     * @param pdf   The PDF document to which the characters belong to.
     * @param page  The page in which the characters are located.
     * @param chars The characters to cut.
     * @return A list of list of characters. In case of the characters could be cut,
     * this list has two inner lists representing the characters of the two
     * halves. In case of the characters could *not* be cut, the list
     * consists only of a single list, which is the original list of
     * characters.
     */
    protected List<ElementList<Character>> xCut(Document pdf, Page page, ElementList<Character> chars) {
        if (chars != null && !chars.isEmpty()) {
            // Sort the characters by minX in order to sweep them in x direction.
            Collections.sort(chars, new MinXComparator());

            // The score of the best cut found so far.
            float bestCutScore = 0;
            // The index of the best cut found so far.
            int bestCutIndex = -1;
            // The current position in the list of characters.
            float currentPos = chars.get(0).getPosition().getRectangle().getMaxX();

            for (int index = 1; index < chars.size(); index++) {
                Character character = chars.get(index);

                if (character.getPosition().getRectangle().getMinX() > currentPos) {
                    List<ElementList<Character>> halves = chars.cut(index);
                    // Find the position of the "best" cut.
                    while (index < chars.size()) {
                        // The score of the current cut.
                        float cutScore = assessVerticalCut(pdf, page, halves);

                        if (cutScore < 0) {
                            break;
                        } else if (cutScore > bestCutScore) {
                            bestCutScore = cutScore;
                            bestCutIndex = index;
                        }
                        halves = chars.cut(++index);
                    }
                }
                currentPos = character.getPosition().getRectangle().getMaxX();
            }

            if (bestCutIndex > -1) {
                // A cut was found. Return the resulting halves.
                return chars.cut(bestCutIndex);
            }
        }
        return Arrays.asList(chars);
    }

    public List<ElementList<Character>> cutTextlinesFast(Document pdf, Page page, ElementList<Character> characters) {
        List<CharBlock> charBlocks = cutIntoBlocks(characters, TextLineCharBlock::new);
        return charBlocks.stream().map(b -> b.elements).collect(Collectors.toList());

    }

    public List<ElementList<Character>> cutWordsFast(Document pdf, Page page, ElementList<Character> characters) {
        List<CharBlock> charBlocks = cutIntoBlocks(characters, WordCharBlock::new);
        return charBlocks.stream().map(b -> b.elements).collect(Collectors.toList());

    }

    // TODO increase the epsilon in the y direction as block could often be merged
    // with block above
    public List<ElementList<Character>> cutTextareasFast(Document pdf, Page page, ElementList<Character> characters) {
        List<CharBlock> charBlocks = cutIntoBlocks(characters, TextAreaCharBlock::new);
        List<ElementList<Character>> result = mergeOverlappingBlocks(charBlocks);

        return result;
    }

    private List<ElementList<Character>> mergeOverlappingBlocks(List<CharBlock> charBlocks) {
        // TODO extract to method. Also: do we have to do this recusively?
        // Second part. merge blocks that overlap
        List<CharBlock> removals = new ArrayList<>();

        for (int i = 0; i < charBlocks.size(); i++) {
            CharBlock block = charBlocks.get(i);
            for (int j = i + 1; j < charBlocks.size(); j++) {
                CharBlock compareBlock = charBlocks.get(j);

                boolean blocksOverlap = intersectsInEpsilonDistance(compareBlock.getEpsilonX(),
                        compareBlock.getEpsilonX(), compareBlock.getEpsilonX(), block.minX, block.minY, block.maxX,
                        block.maxY, compareBlock.minX, compareBlock.minY, compareBlock.maxX, compareBlock.maxY);

                if (blocksOverlap) {
                    block.merge(compareBlock);
                    removals.add(compareBlock);
                }
            }
        }
        charBlocks.removeAll(removals);

        return charBlocks.stream().map(b -> b.elements).collect(Collectors.toList());

    }

    private List<CharBlock> cutIntoBlocks(ElementList<Character> characters, CharBlockConstructor constr) {
        List<CharBlock> charBlocks = new ArrayList<XYCut.CharBlock>();

        for (Character character : characters) {
            CharBlock blockToAssignTo = null;

            for (CharBlock block : charBlocks) {
                if (block.doesFit(character)) {
                    blockToAssignTo = block;
                    break;
                }
            }

            if (blockToAssignTo == null) {
                blockToAssignTo = constr.get(character); // new TextAreaCharBlock(character);
                charBlocks.add(blockToAssignTo);
            } else {
                blockToAssignTo.add(character);
            }

        }
        return charBlocks;
    }

    /**
     * Takes a set of characters and sweeps the characters in y direction in order
     * to find a position to cut the characters vertically into a upper half and a
     * lower half. For more details about the approach of the sweep algorithm, see
     * the examples given for xCut().
     *
     * @param pdf   The PDF document to which the characters belong to.
     * @param page  The page in which the characters are located.
     * @param chars The characters to cut.
     * @return A list of set of characters. In case of the characters could be cut,
     * this list consists of two inner sets, containing the characters of
     * the two halves. In case of the characters could not be cut, the list
     * consists only of a single set, representing a copy of the original
     * set of characters.
     */
    protected List<ElementList<Character>> yCut(Document pdf, Page page, ElementList<Character> chars) {
        if (chars != null && !chars.isEmpty()) {
            // Sort the characters by minX in order to sweep them in x direction.
            Collections.sort(chars, Collections.reverseOrder(new MaxYComparator()));

            // The score of the best cut found so far.
            float bestCutScore = 0;
            // The index of the best cut found so far.
            int bestCutIndex = -1;
            // The current position in the list of characters.
            float currentPos = chars.get(0).getPosition().getRectangle().getMinY();

            for (int index = 1; index < chars.size(); index++) {
                Character character = chars.get(index);

                if (character.getPosition().getRectangle().getMaxY() < currentPos) {
                    List<ElementList<Character>> halves = chars.cut(index);
                    // Find the position of the "best" cut.
                    while (index < chars.size()) {
                        float cutScore = assessHorizontalCut(pdf, page, halves);

                        if (cutScore < 0) {
                            break;
                        } else if (cutScore > bestCutScore) {
                            bestCutScore = cutScore;
                            bestCutIndex = index;
                        }
                        halves = chars.cut(++index);
                    }
                }
                currentPos = character.getPosition().getRectangle().getMinY();
            }

            if (bestCutIndex > -1) {
                // A cut was found. Return the resulting halves.
                return chars.cut(bestCutIndex);
            }
        }
        return Arrays.asList(chars);
    }

    // ==============================================================================================
    // Abstract methods.

    /**
     * Assesses the given vertical cut. Returns a positive score, if the cut is
     * valid and a negative score if the cut is invalid. The better the cut, the
     * higher the returned score.
     *
     * @param pdf    The PDF document to which the characters belong to.
     * @param page   The page in which the characters are located.
     * @param halves The characters of the two halves.
     * @return A score that assesses the given cut.
     */
    public abstract float assessVerticalCut(Document pdf, Page page, List<ElementList<Character>> halves);

    /**
     * Assesses the given horizontal cut. Returns a positive score, if the cut is
     * valid and a negative score if the cut is invalid. The better the cut, the
     * higher the returned score.
     *
     * @param pdf    The PDF document to which the characters belong to.
     * @param page   The page in which the characters are located.
     * @param halves The characters of the two halves.
     * @return A score that assesses the given cut.
     */
    public abstract float assessHorizontalCut(Document pdf, Page page, List<ElementList<Character>> halves);

    // /**
    // * Packs the given characters into the target type.
    // *
    // * @param page
    // * The page in which the characters are located.
    // * @param characters
    // * The characters to pack.
    // *
    // * @return An object of given target type.
    // */
    // public abstract T pack(PdfPage page, PdfElementList<Character> characters);

    // intersection https://www.programmersought.com/article/44554760896/
    private static boolean intersectsInEpsilonDistance(float epsilonX, float epsilonY, float epsilonDiag,
                                                       float rect1Xmin, float rect1Ymin, float rect1Xmax, float rect1Ymax, float rect2Xmin, float rect2Ymin,
                                                       float rect2Xmax, float rect2Ymax) {

        float rect1Width = rect1Xmax - rect1Xmin;
        float rect1Height = rect1Ymax - rect1Ymin;

        float centerRect1X = rect1Xmin + rect1Width / 2;
        float centerRect1Y = rect1Ymin + rect1Height / 2;

        float rect2Width = rect2Xmax - rect2Xmin;
        float rect2Height = rect2Ymax - rect2Ymin;

        float centerRect2X = rect2Xmin + rect2Width / 2;
        float centerRect2Y = rect2Ymin + rect2Height / 2;

        float dX = Math.abs(centerRect1X - centerRect2X);
        float dY = Math.abs(centerRect1Y - centerRect2Y);

        float minDistX = Float.MAX_VALUE;
        float minDistY = Float.MAX_VALUE;
        float minDistDiag = Float.MAX_VALUE;

        boolean minDistXGiven = (dX < ((rect1Width + rect2Width) / 2));
        boolean minDistYGiven = (dY < ((rect1Height + rect2Height) / 2));

        if (minDistXGiven && !minDistYGiven) {
            minDistY = dY - ((rect1Height + rect2Height) / 2);
        } else if (!minDistXGiven && minDistYGiven) {
            minDistX = dX - ((rect1Width + rect2Width) / 2);
        } else if ((dX >= ((rect1Width + rect2Width) / 2)) && (dY >= ((rect1Height + rect2Height) / 2))) {
            float deltaX = dX - ((rect1Width + rect2Width) / 2);
            float deltaY = dY - ((rect1Height + rect2Height) / 2);
            minDistDiag = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        }
        // The intersection of two rectangles, the minimum distance is negative, return
        // -1
        else {
            minDistDiag = -1;
        }

        return minDistX < epsilonX || minDistY < epsilonY || minDistDiag < epsilonDiag;

    }

    /**
     * @author tsteuer
     */

    private static abstract class CharBlock {

        protected final ElementList<Character> elements = new ElementList<>();

        private float minX = Float.MAX_VALUE;
        private float maxX = Float.MIN_VALUE;
        private float minY = Float.MAX_VALUE;
        private float maxY = Float.MIN_VALUE;

        protected CharBlock(Character character) {
            elements.add(character);
            updateBounds(character);
        }

        public void add(Character character) {
            elements.add(character);
            updateBounds(character);
        }

        private void updateBounds(Character character) {
            Rectangle rect = character.getPosition().getRectangle();
            if (rect.getMaxX() > maxX) {
                this.maxX = rect.getMaxX();
            }

            if (rect.getMaxY() > maxY) {
                this.maxY = rect.getMaxY();
            }

            if (rect.getMinX() < minX) {
                this.minX = rect.getMinX();
            }

            if (rect.getMinY() < minY) {
                this.minY = rect.getMinY();
            }

            updateEpsilon(character);
        }

        public void merge(CharBlock other) {
            for (Character character : other.elements) {
                this.add(character);
            }
        }

        public boolean doesFit(Character character) {
            Rectangle rect = character.getPosition().getRectangle();

            return XYCut.intersectsInEpsilonDistance(this.getEpsilonX(), this.getEpsilonY(), this.getEpsilonDiag(),
                    this.minX, this.minY, this.maxX, this.maxY, rect.getMinX(), rect.getMinY(), rect.getMaxX(),
                    rect.getMaxY());

        }

        protected abstract void updateEpsilon(Character character);

        protected abstract float getEpsilonX();

        protected abstract float getEpsilonY();

        protected abstract float getEpsilonDiag();

        @Override
        public String toString() {
            List<String> l = elements.stream().map(c -> c.getText()).collect(Collectors.toList());
            return String.join("", l);
        }

    }

    private static class TextAreaCharBlock extends CharBlock {

        private static final float SPACING = 1.5f;

        private float epsilon;

        public TextAreaCharBlock(Character character) {
            super(character);
            this.epsilon = SPACING * character.getPosition().getRectangle().getHeight();
        }

        @Override
        protected void updateEpsilon(Character character) {
            Rectangle rect = character.getPosition().getRectangle();
            this.epsilon = this.epsilon + (SPACING * rect.getHeight() - this.epsilon) / elements.size();

        }

        @Override
        protected float getEpsilonX() {
            return epsilon;
        }

        @Override
        protected float getEpsilonY() {
            return epsilon;
        }

        @Override
        protected float getEpsilonDiag() {
            return epsilon;
        }
    }

    private interface CharBlockConstructor {
        CharBlock get(Character c);
    }

    private static class TextLineCharBlock extends CharBlock {

        private static final float SPACING = 0.3f;
        private float epsilon;

        public TextLineCharBlock(Character character) {
            super(character);
            this.epsilon = SPACING * character.getPosition().getRectangle().getHeight();
        }

        @Override
        protected void updateEpsilon(Character character) {
            Rectangle rect = character.getPosition().getRectangle();
            this.epsilon = this.epsilon + (SPACING * rect.getHeight() - this.epsilon) / elements.size();

        }

        @Override
        protected float getEpsilonX() {
            return Float.MAX_VALUE;
        }

        @Override
        protected float getEpsilonY() {
            return Float.MIN_VALUE;
        }

        @Override
        protected float getEpsilonDiag() {
            return this.epsilon;
        }

    }

    private static class WordCharBlock extends CharBlock {

        private static final float SPACING = 0.3f;
        private float epsilon;

        public WordCharBlock(Character character) {
            super(character);
            this.epsilon = SPACING * character.getPosition().getRectangle().getHeight();
        }

        @Override
        protected void updateEpsilon(Character character) {
            Rectangle rect = character.getPosition().getRectangle();
            this.epsilon = this.epsilon + (SPACING * rect.getHeight() - this.epsilon) / elements.size();

        }

        @Override
        protected float getEpsilonX() {
            return epsilon;
        }

        @Override
        protected float getEpsilonY() {
            return Float.MAX_VALUE;
        }

        @Override
        protected float getEpsilonDiag() {
            return this.epsilon;
        }

    }

//	private static class CharBlock {
//
//		private static final float SPACING = 1.5f;
//
//		private ElementList<Character> elements = new ElementList<>();
//		private float minX = Float.MAX_VALUE;
//		private float maxX = Float.MIN_VALUE;
//		private float minY = Float.MAX_VALUE;
//		private float maxY = Float.MIN_VALUE;
//		private float epsilon;
//
//		public CharBlock(Character character) {
//			elements.add(character);
//			updateBounds(character);
//			this.epsilon = SPACING * character.getPosition().getRectangle().getHeight();
//		}
//
//		private void updateBounds(Character character) {
//			Rectangle rect = character.getPosition().getRectangle();
//			if (rect.getMaxX() > maxX) {
//				this.maxX = rect.getMaxX();
//			}
//
//			if (rect.getMaxY() > maxY) {
//				this.maxY = rect.getMaxY();
//			}
//
//			if (rect.getMinX() < minX) {
//				this.minX = rect.getMinX();
//			}
//
//			if (rect.getMinY() < minY) {
//				this.minY = rect.getMinY();
//			}
//
//			this.epsilon = this.epsilon + (SPACING * rect.getHeight() - this.epsilon) / elements.size();
//		}
//
//		@Override
//		public String toString() {
//			List<String> l = elements.stream().map(c -> c.getText()).collect(Collectors.toList());
//			return String.join("", l);
//		}
//
//		public boolean doesFit(Character character) {
//			Rectangle rect = character.getPosition().getRectangle();
//
//			return XYCut.intersectsInEpsilonDistance(this.epsilon, this.epsilon, this.epsilon, this.minX, this.minY,
//					this.maxX, this.maxY, rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY());
//
//		}
//
//		public void add(Character character) {
//			elements.add(character);
//			updateBounds(character);
//		}
//
//		public void merge(CharBlock other) {
//			for (Character character : other.elements) {
//				this.add(character);
//			}
//		}

}
