// dto/TrainProgress.java
package com.example.classifierapi.dto;
public record TrainProgress(
    String phase,      // "start" | "split" | "leaf" | "done" | "error"
    int builtNodes,
    int depth,
    String feature,    // best split feature (optional)
    double threshold,  // best split threshold (optional)
    int leftCount,
    int rightCount
) {}
