package pdfact.cli.pipes.index;

import pdfact.core.model.TextLine;
import pdfact.core.util.IndexEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexEntryTransformation {

    private final Pattern LITERALS_ONLY_PATTERN = Pattern.compile("[a-zA-Z]");

    private final Pattern ROMAN_NUMERALS_PATTERN = Pattern
            .compile("^(?i)(M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(,|\\s*)\\s+.*$");

    public final static String DASH_REGEX = "\\u002d|\\u05BE|\\u1806|[\\u2010-\\u2015]|[\\u2E3A-\\u2E3B]|\\uFE58|\\uFE63|\\uFF0D";

    private final List<TextLine> lines;

    public IndexEntryTransformation(List<TextLine> lines) {
        this.lines = lines;
    }

    public Optional<IndexEntry> apply() {
        IndexEntry mainEntry = null;

        String currentLine = lines.get(0).getText().strip();
        StringBuilder entryText = new StringBuilder(currentLine);

        for (int i = 1; i < lines.size(); i++) {
            currentLine = lines.get(i).getText().strip();
            if (belongsToSameEntry(entryText.toString(), currentLine)) {
                entryText.append(lines.get(i).getText().strip());
            } else {
                if (entryText.toString().trim().isEmpty())
                    continue;

                IndexEntry entry = parseEntryText(entryText.toString());
                if (mainEntry != null && entry != null) {
                    mainEntry.addSubEntry(entry);
                } else {
                    mainEntry = entry;
                }
                entryText = new StringBuilder(currentLine);
            }
        }

        //case: multiline; no subentries
        if (mainEntry == null && !entryText.toString().strip().isEmpty()) {
            mainEntry = parseEntryText(entryText.toString());
            entryText = new StringBuilder();
        }

        // case: we have an subentry last
        if (!entryText.toString().strip().isEmpty()) {
            IndexEntry entry = parseEntryText(entryText.toString());
            if(entry != null) {
                mainEntry.addSubEntry(entry);
            }
        }

        return Optional.ofNullable(mainEntry);
    }

    private boolean belongsToSameEntry(String entryText, String nextLineText) {
        entryText = entryText.replaceAll(DASH_REGEX, "-");
        nextLineText = nextLineText.replaceAll(DASH_REGEX, "-");

        if (entryText.endsWith("-"))
            return true;

        if (entryText.endsWith("siehe") && nextLineText.startsWith("auch"))
            return true;

        if (entryText.endsWith("siehe auch"))
            return true;

        if (nextLineText.startsWith("siehe auch"))
            return true;

        //page numbers
        if (!LITERALS_ONLY_PATTERN.matcher(nextLineText).find() || ROMAN_NUMERALS_PATTERN.matcher(nextLineText).matches()) {
            return true;
        }

        return false;
    }


    private IndexEntry parseEntryText(String text) {
        text = text.replaceAll(DASH_REGEX, "-");

        if (text.startsWith("-")) {
            text = text.substring(1).strip();
        }

        IndexEntry entry = new IndexEntry();
        return parsePageNumbers(entry, text, "(,\\s*[0-9-]+)");
    }

    static IndexEntry parsePageNumbers(IndexEntry entry, String text, String pattern) {
        Pattern numberPattern = Pattern.compile(pattern);
        Matcher matcher = numberPattern.matcher(text);

        List<String> pageNumbers = new ArrayList<>();
        int startNumbersIndex = Integer.MAX_VALUE;
        while (matcher.find()) {
            String pageNumberMatch = matcher.group(1);
            addPageNumberMatch(pageNumbers, pageNumberMatch);

            if (matcher.start() < startNumbersIndex) {
                startNumbersIndex = matcher.start();
            }
        }

        String entryText = text.strip();
        if (startNumbersIndex < Integer.MAX_VALUE) {
            entryText = text.substring(0, startNumbersIndex).strip();
        }

        if (!entryText.isBlank() && !entryText.matches("[0-9]+")) {
            entryText = normalizePhrase(entryText);
            entry.setPhrase(entryText);
            entry.addNumbers(pageNumbers);
            return entry;
        }
        return null;
    }

    private static String normalizePhrase(String entryText) {
        while(entryText.endsWith(".")){
            entryText = entryText.substring(0, entryText.length()-1).strip();
        }
        return entryText;
    }

    private static void addPageNumberMatch(List<String> pageNumbers, String pageNumberMatch) {
        if (pageNumberMatch.contains("-")) {
            splitPageNumberRange(pageNumbers, pageNumberMatch, "-");
        } else if (pageNumberMatch.contains("/")) {
            splitPageNumberRange(pageNumbers, pageNumberMatch, "/");
        } else {
            pageNumbers.add(cleanPageNumber(pageNumberMatch.strip()));

        }
    }

    private static void splitPageNumberRange(List<String> pageNumbers, String pageNumberMatch, String tok) {
        String[] pageNumberMatches = pageNumberMatch.split(tok);
        for (String rangeMatch : pageNumberMatches) {
            pageNumbers.add(cleanPageNumber(rangeMatch.strip()));

        }
    }

    private static String cleanPageNumber(String pageNumber) {
        return pageNumber.replace(",", "").strip();
    }

    @Override
    public String toString() {
        StringBuilder accu = new StringBuilder();
        for (TextLine t : this.lines) {
            accu.append(t.getText());
        }
        return "IndexEntryTransformation{" +
                ", lines=" + accu.toString() +
                '}';
    }
}
