package com.example.classifierapi.util;

import com.example.classifierapi.core.TextBlock;
import com.opencsv.CSVReaderHeaderAware;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads a CSV with a text column (default "text") and a label column (param). */
public class CsvUtils {

  public static class Dataset {
    public final List<TextBlock> data;
    public final List<String> labels;
    public Dataset(List<TextBlock> data, List<String> labels) {
      this.data = data; this.labels = labels;
    }
  }

  // Add this record to carry parsed data
public static record Parsed(java.util.List<com.example.classifierapi.core.TextBlock> blocks,
                            java.util.List<String> labels) { }

public static Parsed read(java.io.InputStream in,
                          String labelCol,
                          com.example.classifierapi.util.TextBlockFactory factory) throws java.io.IOException {

    // We assume UTF-8 CSV with header row containing "text" and labelCol (e.g., "label")
    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
    String header = br.readLine();
    if (header == null) throw new java.io.IOException("Empty CSV");

    String[] cols = header.split(",", -1);
    int textIdx = -1, labelIdx = -1;
    for (int i = 0; i < cols.length; i++) {
        String h = cols[i].trim().replaceAll("^\"|\"$", ""); // strip quotes
        if (h.equalsIgnoreCase("text")) textIdx = i;
        if (h.equalsIgnoreCase(labelCol)) labelIdx = i;
    }
    if (textIdx < 0 || labelIdx < 0) {
        throw new java.io.IOException("CSV missing required columns: text + " + labelCol);
    }

    java.util.List<com.example.classifierapi.core.TextBlock> blocks = new java.util.ArrayList<>();
    java.util.List<String> labels = new java.util.ArrayList<>();

    String line;
    while ((line = br.readLine()) != null) {
        // naive CSV split; OK for our generated files (no embedded commas except quoted)
        // If your CSVs have commas inside text, you can swap to OpenCSV here.
        String[] parts = splitCsvLine(line, cols.length);
        if (parts == null) continue; // skip bad line

        String rawText = unquote(parts[textIdx]);
        String rawLabel = unquote(parts[labelIdx]);

        if (rawText == null || rawText.isBlank() || rawLabel == null || rawLabel.isBlank()) continue;

        com.example.classifierapi.core.TextBlock tb = factory.fromRaw(rawText);
        blocks.add(tb);
        labels.add(rawLabel);
    }
    return new Parsed(blocks, labels);
}

// minimal CSV splitter that respects simple quotes
private static String[] splitCsvLine(String line, int expectedCols) {
    java.util.List<String> out = new java.util.ArrayList<>(expectedCols);
    StringBuilder cur = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
        char c = line.charAt(i);
        if (c == '"') {
            inQuotes = !inQuotes;
            continue;
        }
        if (c == ',' && !inQuotes) {
            out.add(cur.toString());
            cur.setLength(0);
        } else {
            cur.append(c);
        }
    }
    out.add(cur.toString());
    return out.toArray(new String[0]);
}

private static String unquote(String s) {
    if (s == null) return null;
    s = s.trim();
    if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
        s = s.substring(1, s.length() - 1);
    }
    return s.replace("\"\"", "\"");
}


  public static Dataset readCsv(File csv, String labelCol, String textCol) throws Exception {
    if (labelCol == null || labelCol.isBlank()) labelCol = "label";
    if (textCol == null  || textCol.isBlank())  textCol  = "text";

    List<TextBlock> data = new ArrayList<>();
    List<String> labels = new ArrayList<>();

    try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csv))) {
      Map<String, String> row;
      while ((row = reader.readMap()) != null) {
        String text  = row.get(textCol);
        String label = row.get(labelCol);
        if (text == null || label == null) continue; // skip bad rows
        data.add(TextBlockFactory.fromRaw(text));
        labels.add(label.trim());
      }
    }
    return new Dataset(data, labels);
  }
}
