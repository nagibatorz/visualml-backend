// adapter/ClassifierAdapter.java (only if needed)
package com.example.classifierapi.adapter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import com.example.classifierapi.dto.DecisionStep;
import com.example.classifierapi.core.TextBlock;

public class ClassifierAdapter {
  @FunctionalInterface
  public interface Saver { void save(PrintStream ps); }

  private final Saver saver;

  public ClassifierAdapter(Saver saver) {
    this.saver = saver;
  }

  public static record Result(String label, List<DecisionStep> steps) {}

  public Result classifyWithPath(Object classifier, TextBlock tb) {
    String tree = dump();
    Deque<String> lines = new ArrayDeque<>(Arrays.asList(tree.split("\\R")));
    List<DecisionStep> path = new ArrayList<>();
    String label = walk(lines, tb, path);
    return new Result(label, path);
  }

  private String dump() {
    var bos = new ByteArrayOutputStream();
    try (var ps = new PrintStream(bos)) { saver.save(ps); }
    return bos.toString();
  }

  private String walk(Deque<String> lines, TextBlock tb, List<DecisionStep> path) {
    if (lines.isEmpty()) throw new IllegalStateException("Malformed tree");
    String line = lines.removeFirst();
    if (line.startsWith("Feature: ")) {
      String feature = line.substring("Feature: ".length()).trim();
      String thLine = lines.removeFirst();
      if (!thLine.startsWith("Threshold: "))
        throw new IllegalStateException("Missing threshold line");
      double threshold = Double.parseDouble(thLine.substring("Threshold: ".length()).trim());

      double value = tb.get(feature);
      boolean goLeft = value < threshold;
      path.add(new DecisionStep(feature, threshold, value, goLeft ? "left" : "right"));

      Deque<String> snapshot = new ArrayDeque<>(lines);
      String leftLabel = walk(snapshot, tb, goLeft ? path : new ArrayList<>());
      int consumed = lines.size() - snapshot.size();
      for (int i = 0; i < consumed; i++) lines.removeFirst();

      if (goLeft) return leftLabel;
      return walk(lines, tb, path);
    } else {
      return line.trim();
    }
  }
}
