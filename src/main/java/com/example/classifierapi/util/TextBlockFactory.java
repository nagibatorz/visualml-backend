package com.example.classifierapi.util;

import com.example.classifierapi.core.TextBlock;

import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TextBlockFactory {

  // Small, effective stopword list (expand later if you want)
  private static final Set<String> STOP = Set.of(
      "a","an","the","and","or","but","to","of","in","on","for","at","by","with","from",
      "as","is","are","was","were","be","been","being",
      "this","that","these","those","it","its","i","you","he","she","they","we","me","him","her","them","us",
      "my","your","his","their","our",
      "not","no","yes","do","does","did","doing","done",
      "up","down","over","under","into","out","about","after","before","again","further",
      "then","once","here","there","when","where","why","how"
  );

  @org.springframework.beans.factory.annotation.Autowired
private com.example.classifierapi.util.TextBlockFactory textBlockFactory;


  public static TextBlock fromRaw(String text) {
    if (text == null) text = "";
    String normalized = Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
        .filter(t -> !t.isBlank())
        .filter(t -> !STOP.contains(t))   // drop stopwords
        .collect(Collectors.joining(" "));
    return new TextBlock(normalized);
  }
}
