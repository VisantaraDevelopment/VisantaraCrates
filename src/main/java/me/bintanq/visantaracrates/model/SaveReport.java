package me.bintanq.visantaracrates.model;

import java.util.ArrayList;
import java.util.List;

public class SaveReport {

    public enum ChangeType { ADDED, MODIFIED, REMOVED }

    public record Entry(String category, ChangeType type, String message) {}

    private final List<Entry> entries = new ArrayList<>();

    public void add(String category, ChangeType type, String message) {
        entries.add(new Entry(category, type, message));
    }

    public List<Entry> getEntries() { return entries; }

    public int count() { return entries.size(); }

    public List<String> toConsoleLines() {
        return entries.stream()
                .map(e -> "[" + e.category() + "] " + e.type().name() + " — " + e.message())
                .toList();
    }

    public List<String> toLogLines() { return toConsoleLines(); }
}