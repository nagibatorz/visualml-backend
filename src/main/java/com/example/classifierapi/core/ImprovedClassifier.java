package com.example.classifierapi.core;
import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
/**

CART-style decision tree with enhanced progress reporting for animations
*/
public class ImprovedClassifier {
// Enhanced progress reporting with more detail for animations
public static class TrainProgress {
public String phase;     // "start" | "feature_scan" | "split" | "leaf" | "done" | "error"
public int builtNodes;
public int totalNodes;   // estimated total (for progress bar)
public int depth;
public String feature;
public double threshold;
public int leftCount;
public int rightCount;
public double gini;      // current node gini
public double gain;      // information gain
public String message;   // human-readable message
 public TrainProgress() {}
 public TrainProgress(String phase, int builtNodes, int totalNodes, int depth,
                      String feature, double threshold, int leftCount, int rightCount,
                      double gini, double gain, String message) {
     this.phase = phase;
     this.builtNodes = builtNodes;
     this.totalNodes = totalNodes;
     this.depth = depth;
     this.feature = feature;
     this.threshold = threshold;
     this.leftCount = leftCount;
     this.rightCount = rightCount;
     this.gini = gini;
     this.gain = gain;
     this.message = message;
 }
}
public interface ProgressListener {
void onEvent(TrainProgress ev);
}
// Enhanced Node class with metadata for visualization
public static class Node {
String feature;
double threshold;
Node left, right;
String label;
 // Enhanced statistics for visualization
 int samples = 0;
 double gini = 0.0;
 Map<String,Integer> dist;
 int nodeId;  // for animation tracking
 
 boolean isLeaf() { return label != null; }
}
private Node root;
private int nodeCount = 0;
private int nodeIdCounter = 0;
private int maxDepthObserved = 0;
// Hyper-parameters
private int MAX_DEPTH = 12;
private int MIN_SAMPLES_SPLIT = 2;
private int MIN_SAMPLES_LEAF  = 1;
private double MIN_GAIN = 1e-3;
// Constructors
public ImprovedClassifier(List<TextBlock> X, List<String> y) {
this(X, y, null);
}
public ImprovedClassifier(List<TextBlock> X, List<String> y, ProgressListener listener) {
if (X == null || y == null) throw new IllegalArgumentException("null inputs");
if (X.isEmpty() || y.isEmpty()) throw new IllegalArgumentException("empty inputs");
if (X.size() != y.size()) throw new IllegalArgumentException("size mismatch");
 // Estimate total nodes (rough estimate for progress bar)
 int estimatedNodes = Math.min(X.size() / 2, 100);
 
 // Send initial start event
 if (listener != null) {
     listener.onEvent(new TrainProgress("start", 0, estimatedNodes, 0, 
         null, 0, X.size(), 0, 0, 0, "Starting training with " + X.size() + " samples"));
 }
 
 // Build feature set
 Set<String> features = new HashSet<>();
 for (TextBlock tb : X) {
     for (String f : tb.getFeatures()) features.add(f);
 }
 
 if (listener != null) {
     listener.onEvent(new TrainProgress("feature_scan", 0, estimatedNodes, 0,
         null, 0, 0, 0, 0, 0, "Found " + features.size() + " unique features"));
 }
 
 // Build tree
 this.root = build(X, y, features, 0, listener, estimatedNodes);
 
 // Send completion event
 if (listener != null) {
     listener.onEvent(new TrainProgress("done", nodeCount, nodeCount, maxDepthObserved, 
         null, 0, 0, 0, 0, 0, "Training complete: " + nodeCount + " nodes, depth " + maxDepthObserved));
 }
}
public ImprovedClassifier(Scanner sc) {
if (sc == null) throw new IllegalArgumentException("null scanner");
this.root = readPreOrder(sc);
this.nodeCount = countNodes(this.root);
this.maxDepthObserved = depth(this.root);
}
// Public API
public String classify(TextBlock tb) {
if (tb == null) return null;
Node n = root;
while (n != null && !n.isLeaf()) {
double v = tb.get(n.feature);
n = (v < n.threshold) ? n.left : n.right;
}
return n != null ? n.label : null;
}
// Enhanced classify with path tracking for animation
public ClassifyResult classifyWithPath(TextBlock tb) {
if (tb == null) return new ClassifyResult(null, new ArrayList<>());
 List<PathStep> path = new ArrayList<>();
 Node n = root;
 
 while (n != null && !n.isLeaf()) {
     double v = tb.get(n.feature);
     boolean goLeft = v < n.threshold;
     
     path.add(new PathStep(
         n.nodeId,
         n.feature,
         n.threshold,
         v,
         goLeft ? "left" : "right",
         n.samples,
         n.gini
     ));
     
     n = goLeft ? n.left : n.right;
 }
 
 String label = n != null ? n.label : null;
 if (n != null && n.isLeaf()) {
     path.add(new PathStep(n.nodeId, null, 0, 0, "leaf", n.samples, 0));
 }
 
 return new ClassifyResult(label, path);
}
public static class ClassifyResult {
public final String label;
public final List<PathStep> path;
public ClassifyResult(String label, List<PathStep> path) {
this.label = label;
this.path = path;
}
}
public static class PathStep {
public final int nodeId;
public final String feature;
public final double threshold;
public final double value;
public final String direction;
public final int samples;
public final double gini;
 public PathStep(int nodeId, String feature, double threshold, double value, 
                String direction, int samples, double gini) {
     this.nodeId = nodeId;
     this.feature = feature;
     this.threshold = threshold;
     this.value = value;
     this.direction = direction;
     this.samples = samples;
     this.gini = gini;
 }
}
public void save(PrintStream out) {
if (out == null) return;
writePreOrder(root, out);
}
public int nodeCount() { return nodeCount; }
public int depth() { return maxDepthObserved; }
public Node getRoot() { return root; }
public Map<String, Double> calculateAccuracy(List<TextBlock> data, List<String> labels) {
if (data == null || labels == null || data.size() != labels.size() || data.isEmpty())
return Map.of();
 Map<String, Integer> total = new HashMap<>();
 Map<String, Integer> correct = new HashMap<>();
 
 for (int i = 0; i < data.size(); i++) {
     String gold = labels.get(i);
     String pred = classify(data.get(i));
     total.merge(gold, 1, Integer::sum);
     if (gold != null && gold.equals(pred)) correct.merge(gold, 1, Integer::sum);
 }
 
 Map<String, Double> acc = new HashMap<>();
 for (var e : total.entrySet()) {
     int t = e.getValue();
     int c = correct.getOrDefault(e.getKey(), 0);
     acc.put(e.getKey(), t == 0 ? 0.0 : (double) c / t);
 }
 
 // Add overall accuracy
 int totalCount = data.size();
 int correctCount = correct.values().stream().mapToInt(Integer::intValue).sum();
 acc.put("Overall", totalCount == 0 ? 0.0 : (double) correctCount / totalCount);
 
 return acc;
}
// Training with progress reporting
private Node build(List<TextBlock> X, List<String> y, Set<String> features,
int depth, ProgressListener listener, int estimatedTotal) {
Node node = new Node();
node.nodeId = nodeIdCounter++;
node.samples = y.size();
node.dist = labelDist(y);
node.gini = gini(y);
nodeCount++;
maxDepthObserved = Math.max(maxDepthObserved, depth);
 // Determine majority label
 String majority = majorityLabel(y);
 
 // Check stopping conditions
 if (depth >= MAX_DEPTH || y.size() < MIN_SAMPLES_SPLIT || node.gini == 0.0) {
     node.label = majority;
     if (listener != null) {
         listener.onEvent(new TrainProgress("leaf", nodeCount, estimatedTotal, depth,
             null, 0, 0, 0, node.gini, 0, 
             "Created leaf node with label: " + majority + " (" + y.size() + " samples)"));
         
         // Small delay for animation visibility
         try { Thread.sleep(100); } catch (InterruptedException e) {}
     }
     return node;
 }

 // Report we're scanning for best split
 if (listener != null && nodeCount % 3 == 1) { // Don't report every single scan
     listener.onEvent(new TrainProgress("feature_scan", nodeCount, estimatedTotal, depth,
         null, 0, y.size(), 0, node.gini, 0,
         "Scanning " + features.size() + " features for best split at depth " + depth));
 }

 // Find best split
 double bestGain = 0.0;
 String bestFeature = null;
 double bestThreshold = 0.0;
 SplitData bestSplit = null;
 
 for (String f : features) {
     double[] vals = new double[X.size()];
     for (int i = 0; i < X.size(); i++) vals[i] = X.get(i).get(f);
     double[] thresholds = candidateThresholds(vals);
     
     for (double t : thresholds) {
         SplitData sd = splitOn(X, y, f, t);
         if (sd.leftY.size() < MIN_SAMPLES_LEAF || sd.rightY.size() < MIN_SAMPLES_LEAF) 
             continue;
             
         double gain = informationGain(y, sd.leftY, sd.rightY);
         if (gain > bestGain + 1e-12) {
             bestGain = gain;
             bestFeature = f;
             bestThreshold = t;
             bestSplit = sd;
         }
     }
 }

 if (bestFeature == null || bestGain < MIN_GAIN) {
     node.label = majority;
     if (listener != null) {
         listener.onEvent(new TrainProgress("leaf", nodeCount, estimatedTotal, depth,
             null, 0, 0, 0, node.gini, 0,
             "No good split found, creating leaf: " + majority));
         
         // Small delay for animation
         try { Thread.sleep(100); } catch (InterruptedException e) {}
     }
     return node;
 }

 node.feature = bestFeature;
 node.threshold = bestThreshold;
 
 if (listener != null) {
     listener.onEvent(new TrainProgress("split", nodeCount, estimatedTotal, depth,
         bestFeature, bestThreshold, bestSplit.leftY.size(), bestSplit.rightY.size(),
         node.gini, bestGain,
         String.format("Split on '%s' < %.4f (gain: %.4f, left: %d, right: %d)", 
             bestFeature, bestThreshold, bestGain, bestSplit.leftY.size(), bestSplit.rightY.size())));
     
     // Small delay for animation visibility
     try { Thread.sleep(150); } catch (InterruptedException e) {}
 }

 // Recursive build
 node.left  = build(bestSplit.leftX,  bestSplit.leftY,  features, depth + 1, listener, estimatedTotal);
 node.right = build(bestSplit.rightX, bestSplit.rightY, features, depth + 1, listener, estimatedTotal);
 
 return node;
}
// Helper classes and methods
private static class SplitData {
List<TextBlock> leftX, rightX;
List<String> leftY, rightY;
SplitData(List<TextBlock> lx, List<String> ly, List<TextBlock> rx, List<String> ry) {
leftX = lx; leftY = ly; rightX = rx; rightY = ry;
}
}
private SplitData splitOn(List<TextBlock> X, List<String> y, String feature, double threshold) {
List<TextBlock> lx = new ArrayList<>();
List<TextBlock> rx = new ArrayList<>();
List<String> ly = new ArrayList<>();
List<String> ry = new ArrayList<>();
for (int i = 0; i < X.size(); i++) {
double v = X.get(i).get(feature);
if (v < threshold) {
lx.add(X.get(i));
ly.add(y.get(i));
} else {
rx.add(X.get(i));
ry.add(y.get(i));
}
}
return new SplitData(lx, ly, rx, ry);
}
private static double[] candidateThresholds(double[] vals) {
double[] copy = Arrays.copyOf(vals, vals.length);
Arrays.sort(copy);
List<Double> t = new ArrayList<>();
for (int i = 1; i < copy.length; i++) {
if (copy[i] != copy[i-1]) t.add((copy[i] + copy[i-1]) / 2.0);
}
if (t.isEmpty()) return new double[] { copy.length > 0 ? copy[0] : 0.0 };
double[] arr = new double[t.size()];
for (int i = 0; i < t.size(); i++) arr[i] = t.get(i);
return arr;
}
private static double gini(List<String> y) {
if (y.isEmpty()) return 0.0;
Map<String, Long> counts = y.stream()
.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
double n = y.size();
double sumSq = 0.0;
for (long c : counts.values()) {
double p = c / n;
sumSq += p * p;
}
return 1.0 - sumSq;
}
private static double informationGain(List<String> parent, List<String> left, List<String> right) {
double gParent = gini(parent);
double n = parent.size();
double wLeft = left.size() / n;
double wRight = right.size() / n;
return gParent - (wLeft * gini(left) + wRight * gini(right));
}
private static String majorityLabel(List<String> y) {
if (y.isEmpty()) return null;
Map<String, Long> counts = y.stream()
.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
return counts.entrySet().stream()
.max(Comparator.comparingLong(Map.Entry::getValue))
.get().getKey();
}
private static Map<String,Integer> labelDist(List<String> y) {
Map<String,Integer> m = new HashMap<>();
for (String s : y) m.merge(s, 1, Integer::sum);
return m;
}
private int countNodes(Node n) {
if (n == null) return 0;
return 1 + countNodes(n.left) + countNodes(n.right);
}
private int depth(Node n) {
if (n == null || n.isLeaf()) return n == null ? 0 : 1;
return 1 + Math.max(depth(n.left), depth(n.right));
}
private void writePreOrder(Node n, PrintStream out) {
if (n == null) return;
if (n.isLeaf()) {
out.println(n.label);
} else {
out.println("Feature: " + n.feature);
out.println("Threshold: " + n.threshold);
writePreOrder(n.left, out);
writePreOrder(n.right, out);
}
}
private Node readPreOrder(Scanner sc) {
if (!sc.hasNextLine()) return null;
String line = sc.nextLine().trim();
if (line.startsWith("Feature:")) {
Node n = new Node();
n.nodeId = nodeIdCounter++;
n.feature = line.substring("Feature:".length()).trim();
if (!sc.hasNextLine()) throw new IllegalArgumentException("Malformed tree");
String th = sc.nextLine().trim();
if (!th.startsWith("Threshold:")) throw new IllegalArgumentException("Malformed tree");
n.threshold = Double.parseDouble(th.substring("Threshold:".length()).trim());
n.left = readPreOrder(sc);
n.right = readPreOrder(sc);
// Compute samples and gini from children (approximate)
if (n.left != null && n.right != null) {
n.samples = n.left.samples + n.right.samples;
}
return n;
} else {
Node n = new Node();
n.nodeId = nodeIdCounter++;
n.label = line;
n.samples = 100; // Default for loaded trees
return n;
}
}
}