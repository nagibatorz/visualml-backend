package com.example.classifierapi.service;
import com.example.classifierapi.core.ImprovedClassifier;
import com.example.classifierapi.core.TextBlock;
import com.example.classifierapi.dto.ClassifyResponse;
import com.example.classifierapi.dto.DecisionStep;
import com.example.classifierapi.dto.TreeNodeDto;
import com.example.classifierapi.util.CsvUtils;
import com.example.classifierapi.util.CsvUtils.Dataset;
import com.example.classifierapi.util.TextBlockFactory;
import com.example.classifierapi.util.TreeParser;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
@Service
public class ClassifierService {
private volatile ImprovedClassifier classifier;
public synchronized boolean isReady() {
return classifier != null;
}
public synchronized void loadModel(File file) throws Exception {
try (Scanner sc = new Scanner(file, "UTF-8")) {
this.classifier = new ImprovedClassifier(sc);
}
}
public synchronized String exportModel() {
if (classifier == null) return "";
ByteArrayOutputStream bos = new ByteArrayOutputStream();
try (PrintStream ps = new PrintStream(bos)) {
classifier.save(ps);
}
return bos.toString();
}
public synchronized ClassifyResponse classify(String text) {
ensureReady();
try {
  TextBlock tb = TextBlockFactory.fromRaw(text);
  
  // Use the new enhanced classifyWithPath method
  ImprovedClassifier.ClassifyResult result = classifier.classifyWithPath(tb);
  
  // Convert PathStep objects to DecisionStep DTOs
  List<DecisionStep> steps = new ArrayList<>();
  if (result != null && result.path != null) {
    for (ImprovedClassifier.PathStep pathStep : result.path) {
      // Only add non-leaf steps to the decision path
      if (pathStep.feature != null) {
        steps.add(new DecisionStep(
          pathStep.feature,
          pathStep.threshold,
          pathStep.value,
          pathStep.direction
        ));
      }
    }
  }
  
  String label = result != null ? result.label : "unknown";
  return new ClassifyResponse(label, steps);
  
} catch (Exception e) {
  System.err.println("Classification error: " + e.getMessage());
  e.printStackTrace();
  return new ClassifyResponse("error", new ArrayList<>());
}
}
public synchronized TreeNodeDto treeDto() {
ensureReady();
ByteArrayOutputStream bos = new ByteArrayOutputStream();
try (PrintStream ps = new PrintStream(bos)) {
classifier.save(ps);
}
String preorder = bos.toString();
return TreeParser.parseFromString(preorder);
}
// Train (blocking) - with better error handling
public synchronized void trainFromCsv(File csv, String labelCol) throws Exception {
try {
System.out.println("Starting training from CSV: " + csv.getName());
System.out.println("Label column: " + labelCol);
  // Parse CSV
  Dataset ds = CsvUtils.readCsv(csv, labelCol, "text");
  
  System.out.println("Loaded " + ds.data.size() + " samples");
  
  if (ds.data.isEmpty()) {
    throw new IllegalArgumentException("CSV has no usable rows. Please ensure it has 'text' and '" + labelCol + "' columns.");
  }
  
  // Validate data
  for (int i = 0; i < Math.min(5, ds.data.size()); i++) {
    System.out.println("Sample " + i + " label: " + ds.labels.get(i));
  }
  
  // Train the model
  System.out.println("Training model...");
  this.classifier = new ImprovedClassifier(ds.data, ds.labels);
  System.out.println("Training complete. Nodes: " + classifier.nodeCount() + ", Depth: " + classifier.depth());
  
} catch (Exception e) {
  System.err.println("Training error: " + e.getMessage());
  e.printStackTrace();
  throw new RuntimeException("Training failed: " + e.getMessage(), e);
}
}
// Train with progress callbacks (SSE) - with better error handling
public synchronized void trainFromCsvWithProgress(File csv, String labelCol,
ImprovedClassifier.ProgressListener listener) throws Exception {
try {
System.out.println("Starting training with progress from CSV: " + csv.getName());
System.out.println("Label column: " + labelCol);
  // Parse CSV
  Dataset ds = CsvUtils.readCsv(csv, labelCol, "text");
  
  System.out.println("Loaded " + ds.data.size() + " samples");
  
  if (ds.data.isEmpty()) {
    throw new IllegalArgumentException("CSV has no usable rows. Please ensure it has 'text' and '" + labelCol + "' columns.");
  }
  
  // Train with progress listener
  this.classifier = new ImprovedClassifier(ds.data, ds.labels, listener);
  System.out.println("Training complete. Nodes: " + classifier.nodeCount() + ", Depth: " + classifier.depth());
  
} catch (Exception e) {
  System.err.println("Training with progress error: " + e.getMessage());
  e.printStackTrace();
  
  // Notify listener of error
  if (listener != null) {
    listener.onEvent(new ImprovedClassifier.TrainProgress(
      "error", 0, 0, 0, null, 0, 0, 0, 0, 0, 
      "Training failed: " + e.getMessage()
    ));
  }
  
  throw new RuntimeException("Training failed: " + e.getMessage(), e);
}
}
// ---- Metrics DTOs ----
public static class ConfusionRow {
public String actual;
public String predicted;
public int count;
public ConfusionRow(String a, String p, int c) {
actual = a;
predicted = p;
count = c;
}
}
public static class Metrics {
public double overall;
public Map<String, Double> perLabel;
public Map<String, Integer> labelCounts;
public List<ConfusionRow> confusion;
public Metrics() {
  this.overall = 0.0;
  this.perLabel = new HashMap<>();
  this.labelCounts = new HashMap<>();
  this.confusion = new ArrayList<>();
}
}
public synchronized Metrics metricsFromCsv(File csv, String labelCol) throws Exception {
ensureReady();
try {
  Dataset ds = CsvUtils.readCsv(csv, labelCol, "text");
  
  if (ds.data.isEmpty()) {
    System.err.println("No data found in CSV for metrics");
    return new Metrics();
  }

  int correct = 0;
  Map<String,Integer> labelCounts = new HashMap<>();
  Map<String,Integer> perCorrect = new HashMap<>();
  Map<String,Integer> perTotal = new HashMap<>();
  Map<String,Map<String,Integer>> confusion = new HashMap<>();

  for (int i = 0; i < ds.data.size(); i++) {
    String actual = ds.labels.get(i);
    String pred = classifier.classify(ds.data.get(i));
    
    if (pred == null) pred = "unknown";
    
    if (Objects.equals(actual, pred)) correct++;
    labelCounts.merge(actual, 1, Integer::sum);
    perTotal.merge(actual, 1, Integer::sum);
    if (Objects.equals(actual, pred)) perCorrect.merge(actual, 1, Integer::sum);
    confusion.computeIfAbsent(actual, k -> new HashMap<>()).merge(pred, 1, Integer::sum);
  }

  Metrics m = new Metrics();
  m.overall = ds.data.isEmpty() ? 0.0 : ((double) correct) / ds.data.size();
  
  for (String lab : perTotal.keySet()) {
    int t = perTotal.getOrDefault(lab, 0);
    int c = perCorrect.getOrDefault(lab, 0);
    m.perLabel.put(lab, t == 0 ? 0.0 : ((double) c) / t);
  }
  m.labelCounts = labelCounts;

  List<ConfusionRow> rows = new ArrayList<>();
  for (var a : confusion.entrySet()) {
    for (var p : a.getValue().entrySet()) {
      rows.add(new ConfusionRow(a.getKey(), p.getKey(), p.getValue()));
    }
  }
  rows.sort(Comparator.comparing((ConfusionRow r) -> r.actual).thenComparing(r -> r.predicted));
  m.confusion = rows;
  
  return m;
  
} catch (Exception e) {
  System.err.println("Metrics calculation error: " + e.getMessage());
  e.printStackTrace();
  throw new RuntimeException("Metrics calculation failed: " + e.getMessage(), e);
}
}
public synchronized int nodeCount() {
return classifier != null ? classifier.nodeCount() : 0;
}
public synchronized int depth() {
return classifier != null ? classifier.depth() : 0;
}
private void ensureReady() {
if (classifier == null) {
throw new IllegalStateException("Model not loaded or trained. Please train a model or upload a saved model first.");
}
}
}