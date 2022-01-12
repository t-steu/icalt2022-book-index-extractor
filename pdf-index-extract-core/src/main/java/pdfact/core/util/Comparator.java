package pdfact.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Comparator {

    private HashMap<String, IndexEntry> pagenumberErrors;
    private HashMap<String, IndexEntry> wrongEntries;
    private HashMap<String, IndexEntry> referenceErrors;
    private HashMap<String, IndexEntry> subEntriesErrors;
    private HashMap<String, IndexEntry> additionalEntries;

    private int numberOfFalseNegative;
    private int numberOfTruePositive;
    private int numberOfFalsePositive;
    private int indexCount;
    private int foundIndexCount;

    public void compare(Collection<IndexEntry> goldStandard, Collection<IndexEntry> input) {
        numberOfFalseNegative = 0;
        boolean entryFound;
        pagenumberErrors = new HashMap<>();
        wrongEntries = new HashMap<>();
        additionalEntries = new HashMap<>();
        referenceErrors = new HashMap<>();
        subEntriesErrors = new HashMap<>();
        // TODO what does indexCount mean?
        // indexCount = input.size();
        indexCount = goldStandard.size();
        foundIndexCount = input.size();
        ArrayList<IndexEntry> copy = new ArrayList<>(input);
        for (IndexEntry goldEntry : goldStandard) {
            entryFound = false;
            for (IndexEntry inputEntry : input) {
                if (inputEntry.getPhrase().equals(goldEntry.getPhrase()) && copy.remove(inputEntry)) {
                    entryFound = true;
                    if (compareToEntriesDebug(goldEntry, inputEntry))
                        numberOfFalseNegative++;
                    // else numberOfTruePositive++;
                    break;
                }
            }
            if (!entryFound) {
                numberOfFalseNegative++;
                wrongEntries.put("Entry was not found: " + goldEntry.getPhrase(), goldEntry);
            }
        }
        if (!copy.isEmpty())
            copy.forEach(entry -> additionalEntries.put("Additional Entry was found: " + entry.getPhrase(), entry));
        numberOfFalsePositive = additionalEntries.size();
        numberOfTruePositive = (indexCount - numberOfFalseNegative);
    }

    public void compareNonStrict(Collection<IndexEntry> goldStandard, Collection<IndexEntry> input) {
        ArrayList<String> goldPhrases = new ArrayList<>();
        ArrayList<String> inputPhrases = new ArrayList<>();
        goldStandard.forEach(e -> {
            goldPhrases.add(e.getPhrase());
        });
        input.forEach(e -> {
            inputPhrases.add(e.getPhrase());
        });
        for (String gPhrase : goldPhrases) {
            if (!inputPhrases.contains(gPhrase))
                numberOfFalseNegative++;
            else
                numberOfTruePositive++;
        }

        for (String iPhrase : inputPhrases) {
            if (!goldPhrases.contains(iPhrase))
                numberOfFalsePositive++;
        }

    }

    private boolean compareToEntries(IndexEntry e1, IndexEntry e2) {
        if (e1.getReference() == null || e2.getReference() == null)
            return e1.getPhrase().equals(e2.getPhrase()) && e1.getNumbers().containsAll(e2.getNumbers());
        else
            return e1.getPhrase().equals(e2.getPhrase()) && e1.getNumbers().containsAll(e2.getNumbers())
                    && e1.getReference().containsAll(e2.getReference());
    }

    private boolean compareToEntriesDebug(IndexEntry e1, IndexEntry e2) {
        ArrayList<String> copy = new ArrayList<>(e2.getNumbers());
        boolean entryIsFalse = false;
        for (String number : e1.getNumbers()) {
            if (!copy.remove(number)) {
                pagenumberErrors.put("Missing page number: " + number, e1);
                entryIsFalse = true;
            }
        }
        if (!copy.isEmpty()) {
            pagenumberErrors.put("Wrong numbers: " + String.join(", ", copy), e1);
            entryIsFalse = true;
        }
        if (e1.getReference() != null && e2.getReference() != null) {
            copy = new ArrayList<>(e2.getReference());
            for (String ref : e1.getReference()) {
                if (!copy.remove(ref)) {
                    referenceErrors.put("Missing references: " + ref, e1);
                    entryIsFalse = true;
                }
            }
            if (!copy.isEmpty()) {
                referenceErrors.put("Wrong references: " + String.join(", ", copy), e1);
                entryIsFalse = true;
            }
        } else if (e1.getReference() == null && e2.getReference() != null) {
            referenceErrors.put("References found but no expected", e1);
            entryIsFalse = true;
        } else if ((e2.getReference() == null && e1.getReference() != null)) {
            referenceErrors.put("No references where found but expected at least one", e1);
            entryIsFalse = true;
        }

        if (e1.getSubentries() != null && e2.getSubentries() != null) {
            copy = new ArrayList<>();
            for (IndexEntry e : e2.getSubentries()) {
                copy.add(e.getPhrase());
            }
            for (int i = 0; i < e1.getSubentries().size() && i < e2.getSubentries().size(); i++) {
                if (compareToEntries(e1.getSubentries().get(i), e2.getSubentries().get(i)))
                    copy.remove(e1.getSubentries().get(i).getPhrase());
                else {
                    subEntriesErrors.put("Something wrong with sub-entry: " + e2.getSubentries().get(i).getPhrase()
                            + " - Required: " + e1.getSubentries().get(i).getPhrase(), e1);
                    entryIsFalse = true;
                }
            }
            if (e1.getSubentries().size() < e2.getSubentries().size()) {
                subEntriesErrors.put("Additional sub-entries found", e1);
                entryIsFalse = true;
            } else if (e1.getSubentries().size() > e2.getSubentries().size()) {
                subEntriesErrors.put("Missing sub-entries", e1);
                entryIsFalse = true;
            }
        } else if (e1.getSubentries() == null && e2.getSubentries() != null) {
            subEntriesErrors.put("Sub-entry found but no expected", e1);
            entryIsFalse = true;
        } else if (e2.getSubentries() == null && e1.getSubentries() != null) {
            subEntriesErrors.put("No sub-entries where found but expected at least one", e1);
            entryIsFalse = true;
        }
        return entryIsFalse;
    }

    public void printResult(boolean printAllStats, boolean strict) throws InterruptedException {
        if (printAllStats) {
            System.err.println("Missing or completely wrong entries");
            wrongEntries.forEach(
                    (key1, value1) -> System.out.println("Entry: " + value1.getPhrase() + " - Errormessage: " + key1));
            Thread.sleep(100);
            System.err.println("Additional found entries");
            additionalEntries.forEach(
                    (key1, value1) -> System.out.println("Entry: " + value1.getPhrase() + " - Errormessage: " + key1));
            Thread.sleep(100);
            System.err.println("Wrong page numbers");
            pagenumberErrors.forEach(
                    (key, value) -> System.out.println("Entry: " + value.getPhrase() + " - Errormessage: " + key));
            Thread.sleep(100);
            System.err.println("Wrong sub-entries");
            subEntriesErrors.forEach(
                    (key, value) -> System.out.println("Entry: " + value.getPhrase() + " - Errormessage: " + key));
            Thread.sleep(100);
            System.err.println("Wrong references");
            referenceErrors.forEach(
                    (key, value) -> System.out.println("Entry: " + value.getPhrase() + " - Errormessage: " + key));
        }
        System.err.println("Statistics ");
        System.err.println("Gold Index Count:  " + indexCount);
        System.err.println("Found Index Count:  " + foundIndexCount);

        if (!strict) {
            System.err.println("Total count of missing: " + wrongEntries.size());
            System.err.println("Total count of Wrong page numbers: " + pagenumberErrors.size());
            System.err.println("Total count of Wrong sub-entries: " + subEntriesErrors.size());
            System.err.println("Total count of Wrong references: " + referenceErrors.size());
        }

        double recall = numberOfTruePositive / (double) (numberOfTruePositive + numberOfFalseNegative);
        double precision = numberOfTruePositive / (double) (numberOfTruePositive + numberOfFalsePositive);
        System.err.println("True Positive: " + numberOfTruePositive);
        System.err.println("False Negative: " + numberOfFalseNegative);
        System.err.println("False Positive: " + numberOfFalsePositive);
        System.err.println("Precision: " + String.format("%.2f", (precision * 100)) + "%");
        System.err.println("Recall: " + String.format("%.2f", (recall * 100)) + "%");
        System.err.println(
                "F1-Measure: " + String.format("%.2f", ((2 * precision * recall) / (precision + recall)) * 100) + "%");
    }
}
