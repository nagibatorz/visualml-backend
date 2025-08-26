// core/ProgressListener.java
package com.example.classifierapi.core;
import com.example.classifierapi.dto.TrainProgress;
public interface ProgressListener {
  void onEvent(TrainProgress ev);
}
