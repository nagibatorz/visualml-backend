package com.example.classifierapi.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.example.classifierapi.dto.TreeNodeDto;

/**
 * Parses your pre-order text format into a TreeNodeDto:
 * Branch: "Feature: <name>" then "Threshold: <double>"
 * Leaf:   "<label>"
 */
public class TreeParser {

  public static TreeNodeDto parseFromString(String preorder) {
    Deque<String> lines = new ArrayDeque<>(Arrays.asList(preorder.split("\\R")));
    return parse(lines);
  }

  private static TreeNodeDto parse(Deque<String> lines) {
    if (lines.isEmpty()) throw new IllegalStateException("Malformed tree: no more lines");
    String line = lines.removeFirst();

    if (line.startsWith("Feature: ")) {
      String feature = line.substring("Feature: ".length()).trim();
      if (lines.isEmpty()) throw new IllegalStateException("Expected Threshold line after Feature");
      String thLine = lines.removeFirst();
      if (!thLine.startsWith("Threshold: "))
        throw new IllegalStateException("Missing 'Threshold: ' after Feature: " + feature);

      double threshold = Double.parseDouble(thLine.substring("Threshold: ".length()).trim());

      TreeNodeDto node = new TreeNodeDto();
      node.isLeaf = false;
      node.feature = feature;
      node.threshold = threshold;
      node.left = parse(lines);   // preorder left
      node.right = parse(lines);  // preorder right
      return node;
    } else {
      TreeNodeDto leaf = new TreeNodeDto();
      leaf.isLeaf = true;
      leaf.label = line.trim();
      return leaf;
    }
  }
}
