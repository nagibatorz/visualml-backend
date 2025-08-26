package com.example.classifierapi.core;
// Zhorzh Zelenkov
// 05/04/2025
// CSE 123
// P3: Spam Classifier
// TA's: Ido Avnon
// Classifier class represents a text classification decision tree that can predict labels
// for text data. It can be trained from data or loaded from a file, and provides
// methods to classify new text inputs and save the trained model.

import java.io.*;
import java.util.*;

public class Classifier {

    private ClassifierNode overallRoot;

    // Loads a classification tree from the provided Scanner input in pre-order format.
    // The file should contain decision nodes with "Feature: " and "Threshold: " lines
    // followed by leaf nodes containing only labels.
    // Parameters:
    //   - input: Scanner connected to a file containing the tree data
    // Exceptions:
    //   - throws IllegalArgumentException if input is null
    //   - throws IllegalStateException if tree is empty after processing input
    // Returns:
    //   - none
    public Classifier(Scanner input){
        if(input == null){
            throw new IllegalArgumentException();
        }
        overallRoot = loadFromFile(input);
        if(overallRoot == null){
            throw new IllegalStateException();
        }
    }

    // Trains a classification tree from the provided training data and corresponding labels.
    // Processes data in order, making corresponding changes to the tree 
    // when predictions are incorrect
    // Parameters:
    //   - data: List<TextBlock> list with data to train on
    //   - results: List<String> of expected labels corresponding to each TextBlock
    // Exceptions:
    //   - throws IllegalArgumentException 
    //     if data or results is null, 
    //     if they have different sizes, 
    //     if either list is empty
    // Returns:
    //   - none
    public Classifier(List<TextBlock> data, List<String> results){
        if(data == null || results == null || data.size() != results.size() || data.isEmpty()){
            throw new IllegalArgumentException();
        }
        overallRoot = new ClassifierNode(results.get(0), data.get(0));
        for(int i = 1; i < data.size(); i++){
            String predict = classify(data.get(i));
            if(!predict.equals(results.get(i))){
                overallRoot = train(overallRoot, data.get(i), results.get(i));
            }
        }
    }

    // Updates the tree structure when a prediction is incorrect by creating a new
    // decision node based on the biggest difference between TextBlock's and placing
    // the original and new data.
    // Parameters:
    //   - root: ClassifierNode current node being examined
    //   - input: TextBlock that was misclassified
    //   - label: String correct label for the input
    // Returns:
    //   - ClassifierNode: updated tree structure
    private ClassifierNode train(ClassifierNode root, TextBlock input, String label){
        if(root.feature == null){
            String feature = root.data.findBiggestDifference(input);
            double threshold = midpoint(root.data.get(feature), input.get(feature));
            ClassifierNode decision = new ClassifierNode(feature, threshold);
            if(root.data.get(feature) < threshold){
                decision.left = root;
                decision.right = new ClassifierNode(label, input);;
            } else{
                decision.left = new ClassifierNode(label, input);;
                decision.right = root;
            }
            return decision;
        } else{
            if(input.get(root.feature) < root.threshold){
                root.left = train(root.left, input, label);
            } else{
                root.right = train(root.right, input, label);
            }
            return root;
        }
    }

    // Classifies the given TextBlock input
    // Parameters:
    //   - input: TextBlock object to classify
    // Exceptions:
    //   - throws IllegalArgumentException if input is null
    // Returns:
    //   - String: the predicted label for the input text
    public String classify(TextBlock input){
        if(input == null){
            throw new IllegalArgumentException();
        }
        return classify(overallRoot, input);
    }

    // Classifies the given TextBlock input by traversing the decision tree.
    // At each decision node, compares the input's feature value to the threshold
    // and travels left if less than threshold, right if greater than or equal, 
    // very similar to BST structure
    // Parameters:
    //   - input: TextBlock object to classify
    // Returns:
    //   - String: the predicted label for the input text
    private String classify(ClassifierNode root, TextBlock input){
        if(root.feature == null){
            return root.label;
        } else{
            if(input.get(root.feature) < root.threshold){
                return classify(root.left, input);
            } else{
                return classify(root.right, input);
            }
        }
    }

    // Saves the current classification tree to the provided PrintStream in pre-order format.
    // Decision nodes are saved in the format of "Feature: " and "Threshold: " lines,
    // while leaf nodes contain only label.
    // Parameters:
    //   - output: PrintStream to write the tree data to
    // Exceptions:
    //   - throws IllegalArgumentException if output is null
    // Returns:
    //   - none
    public void save(PrintStream output){
        if(output == null){
            throw new IllegalArgumentException();
        }
        save(overallRoot, output);
    }

    // Saves the tree structure using recursion in pre-order format by writing
    // feature and threshold information for decision nodes and labels for leaf nodes.
    // Decision nodes are saved in the format of "Feature: " and "Threshold: " lines,
    // while leaf nodes contain only label.
    // Parameters:
    //   - root: current node being processed
    //   - output: PrintStream to write to
    // Returns:
    //   - none
    private void save(ClassifierNode root, PrintStream output){
        if(root != null){
            if(root.feature == null){
                output.println(root.label);
            } else{
                output.println("Feature: " + root.feature);
                output.println("Threshold: " + root.threshold);
                save(root.left, output);
                save(root.right, output);
            }
        }
    }

    // Recursively loads tree nodes from the Scanner input in pre-order format.
    // Creates decision nodes when encountering "Feature: " lines and leaf nodes
    // for lines only containing the label.
    // Parameters:
    //   - input: Scanner containing tree data
    // Returns:
    //   - ClassifierNode: the root of the loaded subtree, or null if no data
    private ClassifierNode loadFromFile(Scanner input){
        if(!input.hasNextLine()){
            return null;
        }
        String line = input.nextLine();
        if(line.startsWith("Feature: ")){
            String feature = line.substring("Feature: ".length());
            String thresholdLine = input.nextLine();
            double threshold = Double.parseDouble
                                (thresholdLine.substring("Threshold: ".length()));
            ClassifierNode node = new ClassifierNode(feature, threshold, loadFromFile(input), 
                                                    loadFromFile(input));
            return node;
        } else{
            return new ClassifierNode(line, null);
        }
    }

    // ClassifierNode class represents a node in the classification decision tree.
    // It can represent either a decision node that contains a feature and threshold
    // or a leaf node that contains a label and the original training data).
    // Decision nodes have non-null feature fields, while leaf nodes have some null fields
    private static class ClassifierNode{
        public final String feature;
        public final double threshold;
        public final String label;
        public final TextBlock data;
        public ClassifierNode left;
        public ClassifierNode right;

        // Creates a decision node with the specified feature and threshold.
        // Used for internal nodes that make classification decisions.
        // Parameters:
        //   - feature: String feature to test
        //   - threshold: double, probability threshold for the decision
        public ClassifierNode(String feature, double threshold){
            this.feature = feature;
            this.threshold = threshold;
            this.label = null;
            this.data = null;
        }

        public ClassifierNode(String feature, double threshold, ClassifierNode left, 
                                ClassifierNode right){
            this.feature = feature;
            this.threshold = threshold;
            this.label = null;
            this.data = null;
            this.left = left;
            this.right = right;
        }

        // Creates a leaf node with the specified label and training data.
        // Used for terminal nodes that provide classification outcome.
        // Parameters:
        //   - label: String classification label
        //   - data: TextBlock used to create this label
        public ClassifierNode(String label, TextBlock data){
            this.feature = null;
            this.threshold = 0.0;
            this.label = label;
            this.data = data;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // PROVIDED METHODS - **DO NOT MODIFY ANYTHING BELOW THIS LINE!** //
    ////////////////////////////////////////////////////////////////////

    // Helper method to calcualte the midpoint of two provided doubles.
    private static double midpoint(double one, double two) {
        return Math.min(one, two) + (Math.abs(one - two) / 2.0);
    }    

    // Behavior: Calculates the accuracy of this model on provided Lists of 
    //           testing 'data' and corresponding 'labels'. The label for a 
    //           datapoint at an index within 'data' should be found at the 
    //           same index within 'labels'.
    // Exceptions: IllegalArgumentException if the number of datapoints doesn't match the number 
    //             of provided labels
    // Returns: a map storing the classification accuracy for each of the encountered labels when
    //          classifying
    // Parameters: data - the list of TextBlock objects to classify. Should be non-null.
    //             labels - the list of expected labels for each TextBlock object. 
    //             Should be non-null.
    public Map<String, Double> calculateAccuracy(List<TextBlock> data, List<String> labels) {
        // Check to make sure the lists have the same size (each datapoint has an expected label)
        if (data.size() != labels.size()) {
            throw new IllegalArgumentException(
                    String.format("Length of provided data [%d] doesn't match provided labels [%d]",
                                  data.size(), labels.size()));
        }
        
        // Create our total and correct maps for average calculation
        Map<String, Integer> labelToTotal = new HashMap<>();
        Map<String, Double> labelToCorrect = new HashMap<>();
        labelToTotal.put("Overall", 0);
        labelToCorrect.put("Overall", 0.0);
        
        for (int i = 0; i < data.size(); i++) {
            String result = classify(data.get(i));
            String label = labels.get(i);

            // Increment totals depending on resultant label
            labelToTotal.put(label, labelToTotal.getOrDefault(label, 0) + 1);
            labelToTotal.put("Overall", labelToTotal.get("Overall") + 1);
            if (result.equals(label)) {
                labelToCorrect.put(result, labelToCorrect.getOrDefault(result, 0.0) + 1);
                labelToCorrect.put("Overall", labelToCorrect.get("Overall") + 1);
            }
        }

        // Turn totals into accuracy percentage
        for (String label : labelToCorrect.keySet()) {
            labelToCorrect.put(label, labelToCorrect.get(label) / labelToTotal.get(label));
        }
        return labelToCorrect;
    }
}
