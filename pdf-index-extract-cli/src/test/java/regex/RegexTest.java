package regex;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexTest {
    Pattern pattern = Pattern.compile("^[^\\d]((\\S+\\s*)+)(,|\\s+)\\d+$");
    Pattern whitespacePattern = Pattern.compile("^\\s+.+$");
    Pattern onlyNumbers = Pattern.compile("^(\\d+-*)*((,|\\s+)*(\\d+-*–*)*)+\\d+,*$");
    Pattern onlyNumbers2 = Pattern.compile("\\s*\\d+(\\s*|-)\\s*\\d*");
    Pattern onlyLiterals = Pattern.compile("[a-zA-Z][,_-]*");
    Pattern checkNames = Pattern.compile("(?i)(index|verzeichnis|namenverzeichnis|sachverzeichnis|"
            + "namen- und sachverzeichnis|stichwortverzeichnis|register|konkordanz|"
            + "sachwortverzeichnis|sach- und personenregister|Sach- und Namenverzeichnis)");

    public String performIndexTransformation(String phraseLine) {
        String[] splitIndex = phraseLine.split(",");
        if (splitIndex.length == 0)
            return null;
        ArrayList<String> numbers = new ArrayList<>();
        StringBuilder phrase = new StringBuilder();
        for (String s : splitIndex) {
            System.out.println(s);
            if (s.matches("\\s*\\d+\\s*(\\s*|-|–)\\s*\\d*\\s*"))
                numbers.add(s);
            else
                phrase.append(s);
        }
        return phrase.toString();
    }

    @Test
    public void testIndexWithTwoWordsComma() {
        Assert.assertEquals(pattern.matcher("Mandelbrot set, 163").matches(), true);
    }

    @Test
    public void testIndexWithoutTwoWordNoComma() {
        Assert.assertEquals(pattern.matcher("Mandelbrot set  163").matches(), true);
    }

    @Test
    public void testIndexComma() {
        Assert.assertEquals(pattern.matcher("Mandelbrotset, 163").matches(), true);
    }

    @Test
    public void testIndexNoComma() {
        Assert.assertEquals(pattern.matcher("Mandelbrotset 163").matches(), true);
    }

    @Test
    public void testNotIndexEndNotMatching() {
        Assert.assertEquals(pattern.matcher("Mandelbrotset   163abc").matches(), false);
    }

    // Not sure about this case
    @Test
    public void testNotIndexBeginNotMatching() {
        Assert.assertEquals(pattern.matcher(", Mandelbrot set, 163").matches(), false);
    }

    @Test
    public void testIndexWithMultipleWords() {
        Assert.assertEquals(pattern.matcher("Standard model of arithmetic, 43, 46, 147").matches(), true);
    }

    @Test
    public void testNotOnlyDigit() {
        Assert.assertEquals(pattern.matcher("147 147").matches(), false);
        Assert.assertEquals(pattern.matcher(", 147, 147").matches(), false);
        Assert.assertEquals(pattern.matcher(",147").matches(), false);
    }

    @Test
    public void testNotWordWithDigit() {
        Assert.assertEquals(pattern.matcher("Test147").matches(), false);
    }

    @Test
    public void testNotOnlyChar() {
        Assert.assertEquals(pattern.matcher("T").matches(), false);
    }

    @Test
    public void testWhitespacePattern() {
        Assert.assertEquals(whitespacePattern.matcher("   Mandelbrot set, 163").matches(), true);
    }

    @Test
    public void testOnlyNumbers() {
        Assert.assertEquals(onlyNumbers.matcher("178, 163").matches(), true);
        // Assert.assertEquals(onlyNumbers.matcher("xii 178, 163").matches(), true);
        Assert.assertEquals(onlyNumbers.matcher("ABC 178 ABC").matches(), false);
        Assert.assertEquals(onlyNumbers.matcher("ABC").matches(), false);
        Assert.assertEquals(onlyNumbers.matcher("184–186 188").matches(), true);
        Assert.assertEquals(onlyNumbers.matcher("184–186, 188, 189, 192–197, 399, 511,").matches(), true);
        Assert.assertEquals(onlyNumbers.matcher("184, 188, 189, 192–197, 399, 511-12").matches(), true);
        Assert.assertEquals(onlyNumbers.matcher("220, 229, 239, 243, 251, 261–266,").matches(), true);
        Assert.assertEquals(onlyNumbers.matcher("150–152").matches(), true);
        Assert.assertEquals(onlyNumbers.matcher("3D printed part, xi, 77, 79, 93, 101, 106, 267").matches(), false);
        Assert.assertEquals(onlyNumbers.matcher("Binary search tree, 237, 238, 255, 256,").matches(), false);
    }

    @Test
    public void testOnlyNumbers2() {
        // Universal design, 159-161, 167
        // Universal design, 159–161, 167
        // PHrase, 146-146, 765
        System.out.println("Universal design, 159–161, 167".compareTo("Universal design, 159-161, 167"));
        System.out.println(performIndexTransformation("Universal design, 159–161, 167"));
    }

    @Test
    public void testOnlyForLiterals() {
        Assert.assertEquals(onlyLiterals.matcher("178, 163").find(), false);
        Assert.assertEquals(onlyLiterals.matcher("A,BCA_BC").find(), true);
        Assert.assertEquals(onlyLiterals.matcher("Sclera Blood Vessels, Periocular and Iris").find(), true);
        Assert.assertEquals(onlyLiterals.matcher("ABC").find(), true);
        Assert.assertEquals(onlyLiterals.matcher("184–186 188").find(), false);
        Assert.assertEquals(onlyLiterals.matcher("184–186, 188, 189, 192–197, 399, 511,").find(), false);
        Assert.assertEquals(onlyLiterals.matcher("184, 188, 189, 192–197, 399, 511-12").find(), false);
        Assert.assertEquals(onlyLiterals.matcher("220, 229, 239, 243, 251, 261–266,").find(), false);
        Assert.assertEquals(onlyLiterals.matcher("150–152").find(), false);
        Assert.assertEquals(onlyLiterals.matcher("3D printed part, xi, 77, 79, 93, 101, 106, 267").find(), true);
        Assert.assertEquals(onlyLiterals.matcher("Binary search tree, 237, 238, 255, 256,").find(), true);
    }

    @Test
    public void testSplitNumbers() {
        List<String> l = Arrays
                .stream("184 – 186, 188, 189, 192–197, 399, 511,".replace(",", "").replace(" – ", "-").split("\\s+"))
                .collect(Collectors.toList());
        l.forEach(p -> System.out.println(p));
    }

    @Test
    public void testNames() {
        Assert.assertEquals(checkNames.matcher("Stichwortverzeichnis").matches(), true);
    }
}
