package pdfact.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IndexEntry {

    private String phrase;
    private List<String> numbers = new ArrayList<>();
    private List<IndexEntry> subEntries = new ArrayList<>();
    private List<String> references = new ArrayList<>();
    private List<Integer> offsets = new ArrayList<>();

    public IndexEntry() {

    }


    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public void addNumbers(Collection<String> numbers) {
        this.numbers.addAll(numbers);
    }

    public void addSubEntry(IndexEntry subEntry) {
        this.subEntries.add(subEntry);
    }


    public void addReferences(Collection<String> references) {
        this.references.addAll(references);
    }

    public boolean isHasSubEntries() {
        return !this.subEntries.isEmpty();
    }

    public String getPhrase() {
        return phrase;
    }

    public List<String> getNumbers() {
        return numbers;
    }

    public List<IndexEntry> getSubentries() {
        return subEntries;
    }

    public List<String> getReference() {
        return references;
    }

    public void print(String phrase, List<String> numbers, List<IndexEntry> subEntries,
                      List<String> reference, boolean isSub) {
        if (isSub)
            System.out.println("Sub-Entry: " + phrase);
        else
            System.out.println("Entry: " + phrase);
        if (numbers != null || numbers.size() == 0) {
            numbers.forEach(x -> System.out.print(x + " "));
            System.out.print("\n");
        }
        if (reference != null) {
            System.out.println("References:");
            reference.forEach(System.out::println);
        }
        if (subEntries != null) {
            System.out.println("Sub-Entries:");
            subEntries.forEach(x -> print(x.getPhrase(), x.getNumbers(), x.getSubentries(), x.getReference(), true));
        }
    }

    public void addOffsets(List<Integer> offsets) {
        this.offsets.addAll(offsets);
    }

    public List<Integer> getOffsets() {
        return offsets;
    }
}
