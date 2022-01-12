package pdfact.core.util.statistician;

import pdfact.core.model.Character;
import pdfact.core.model.*;
import pdfact.core.util.counter.FloatCounter;
import pdfact.core.util.counter.ObjectCounter;
import pdfact.core.util.list.ElementList;

import java.util.List;

/**
 * A class that computes statistics about characters.
 *
 * @author Claudius Korzen
 */
public class CharacterStatistician {
    /**
     * Computes the character statistic for the given characters.
     *
     * @param hasCharacters An element that has characters.
     * @return The computed character statistics.
     */
    public CharacterStatistic compute(HasCharacters hasCharacters) {
        return compute(hasCharacters.getCharacters());
    }

    /**
     * Computes the character statistic for the given characters.
     *
     * @param characters The characters to process.
     * @return The computed character statistics.
     */
    public CharacterStatistic compute(ElementList<Character> characters) {
        // Create a new statistic object.
        CharacterStatistic statistic = new CharacterStatistic();

        // Initialize counters for the heights, widths and font sizes.
        FloatCounter heightsFrequencies = new FloatCounter();
        FloatCounter widthsFrequencies = new FloatCounter();
        FloatCounter fontsizeFrequencies = new FloatCounter();

        // Initialize counters for the colors and font faces.
        ObjectCounter<Color> colorFreqs = new ObjectCounter<>();
        ObjectCounter<FontFace> fontFreqs = new ObjectCounter<>();

        for (Character character : characters) {
            Position position = character.getPosition();
            Rectangle rectangle = position.getRectangle();

            heightsFrequencies.add(rectangle.getHeight());
            widthsFrequencies.add(rectangle.getWidth());
            fontFreqs.add(character.getFontFace());
            fontsizeFrequencies.add(character.getFontFace().getFontSize());
            colorFreqs.add(character.getColor());

            if (rectangle.getMinX() < statistic.getSmallestMinX()) {
                statistic.setSmallestMinX(rectangle.getMinX());
            }

            if (rectangle.getMinY() < statistic.getSmallestMinY()) {
                statistic.setSmallestMinY(rectangle.getMinY());
            }

            if (rectangle.getMaxX() > statistic.getLargestMaxX()) {
                statistic.setLargestMaxX(rectangle.getMaxX());
            }

            if (rectangle.getMaxY() > statistic.getLargestMaxY()) {
                statistic.setLargestMaxY(rectangle.getMaxY());
            }
        }

        // Fill the statistic object.
        statistic.setHeightFrequencies(heightsFrequencies);
        statistic.setWidthFrequencies(widthsFrequencies);
        statistic.setFontSizeFrequencies(fontsizeFrequencies);
        statistic.setColorFrequencies(colorFreqs);
        statistic.setFontFaceFrequencies(fontFreqs);

        return statistic;
    }

    /**
     * Combines the given list of character statistics to a single statistic.
     *
     * @param stats The statistics to combine.
     * @return The combined statistic.
     */
    public CharacterStatistic aggregate(List<? extends HasCharacterStatistic> stats) {
        // Create new statistic object.
        CharacterStatistic statistic = new CharacterStatistic();

        // Initialize counters for the heights, widths and font sizes.
        FloatCounter heightsFrequencies = new FloatCounter();
        FloatCounter widthsFrequencies = new FloatCounter();
        FloatCounter fontsizeFrequencies = new FloatCounter();

        // Initialize counters for the colors and font faces.
        ObjectCounter<Color> colorFreqs = new ObjectCounter<>();
        ObjectCounter<FontFace> fontFreqs = new ObjectCounter<>();

        // Aggregate the given statistics.
        for (HasCharacterStatistic s : stats) {
            CharacterStatistic stat = s.getCharacterStatistic();

            heightsFrequencies.add(stat.getHeightFrequencies());
            widthsFrequencies.add(stat.getWidthFrequencies());
            fontFreqs.add(stat.getFontFaceFrequencies());
            fontsizeFrequencies.add(stat.getFontSizeFrequencies());
            colorFreqs.add(stat.getColorFrequencies());

            if (stat.getSmallestMinX() < statistic.getSmallestMinX()) {
                statistic.setSmallestMinX(stat.getSmallestMinX());
            }

            if (stat.getSmallestMinY() < statistic.getSmallestMinY()) {
                statistic.setSmallestMinY(stat.getSmallestMinY());
            }

            if (stat.getLargestMaxX() > statistic.getLargestMaxX()) {
                statistic.setLargestMaxX(stat.getLargestMaxX());
            }

            if (stat.getLargestMaxY() > statistic.getLargestMaxY()) {
                statistic.setLargestMaxY(stat.getLargestMaxY());
            }
        }

        // Fill the statistic object.
        statistic.setHeightFrequencies(heightsFrequencies);
        statistic.setWidthFrequencies(widthsFrequencies);
        statistic.setFontSizeFrequencies(fontsizeFrequencies);
        statistic.setColorFrequencies(colorFreqs);
        statistic.setFontFaceFrequencies(fontFreqs);

        return statistic;
    }
}
