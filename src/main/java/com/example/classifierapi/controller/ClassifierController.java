package com.example.classifierapi.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.core.io.ClassPathResource;
import com.example.classifierapi.dto.ClassifyResponse;
import com.example.classifierapi.dto.TreeNodeDto;
import com.example.classifierapi.service.ClassifierService;
import com.example.classifierapi.core.ImprovedClassifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ClassifierController {
private final ClassifierService service;
private final ExecutorService exec = Executors.newCachedThreadPool();
public ClassifierController(ClassifierService service) {
this.service = service;
}
@GetMapping("/ready")
public boolean ready() {
return service.isReady();
}
@PostMapping("/load-model")
public ResponseEntity<Boolean> loadModel(@RequestParam("file") MultipartFile file) throws Exception {
File tmp = File.createTempFile("model-", ".txt");
file.transferTo(tmp);
try {
service.loadModel(tmp);
return ResponseEntity.ok(true);
} finally {
tmp.delete();
}
}
@GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
public String export() {
return service.exportModel();
}
// Accept {"text":"..."} payload directly, no dependency on a getter
@PostMapping("/classify")
public ClassifyResponse classify(@RequestBody Map<String, Object> body) {
Object t = body.get("text");
String text = t == null ? "" : String.valueOf(t);
return service.classify(text);
}
@GetMapping("/tree")
public TreeNodeDto tree() {
return service.treeDto();
}
@PostMapping(value = "/train", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Boolean> train(
@RequestParam("file") MultipartFile file,
@RequestParam(value = "labelCol", defaultValue = "label") String labelCol
) throws Exception {
File tmp = File.createTempFile("train-", ".csv");
file.transferTo(tmp);
try {
service.trainFromCsv(tmp, labelCol);
return ResponseEntity.ok(true);
} finally {
tmp.delete();
}
}
@PostMapping(
value = "/train/stream",
consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter trainStream(
@RequestParam("file") MultipartFile file,
@RequestParam(value = "labelCol", defaultValue = "label") String labelCol
) throws Exception {
File tmp = File.createTempFile("train-", ".csv");
file.transferTo(tmp);
SseEmitter emitter = new SseEmitter(0L);
exec.execute(() -> {
  try {
    service.trainFromCsvWithProgress(tmp, labelCol, ev -> {
      try { emitter.send(SseEmitter.event().name("progress").data(ev)); } catch (Exception ignore) {}
    });
    emitter.send(SseEmitter.event().name("progress").data(
        new ImprovedClassifier.TrainProgress("done", service.nodeCount(), service.nodeCount(), 
            service.depth(), null, 0, 0, 0, 0, 0, 
            "Training complete: " + service.nodeCount() + " nodes, depth " + service.depth())
    ));
    emitter.complete();
  } catch (Exception ex) {
    try {
      emitter.send(SseEmitter.event().name("progress")
          .data(new ImprovedClassifier.TrainProgress("error", 0, 0, 0, null, 0, 0, 0, 0, 0, 
              "Error: " + ex.getMessage())));
    } catch (Exception ignore) {}
    emitter.completeWithError(ex);
  } finally {
    tmp.delete();
  }
});
return emitter;
}
@PostMapping(value = "/metrics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ClassifierService.Metrics> metrics(
@RequestParam("file") MultipartFile file,
@RequestParam(value = "labelCol", defaultValue = "label") String labelCol
) throws Exception {
File tmp = File.createTempFile("test-", ".csv");
file.transferTo(tmp);
try {
var m = service.metricsFromCsv(tmp, labelCol);
return ResponseEntity.ok(m);
} finally {
tmp.delete();
}
}
// Download sample files endpoint
@GetMapping(value = "/download/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) throws IOException {
// Only allow specific files for security
if (!filename.equals("sms_tree.txt") && !filename.equals("test.csv")) {
return ResponseEntity.notFound().build();
}
// Load from classpath resources
ClassPathResource resource = new ClassPathResource("static/samples/" + filename);

if (!resource.exists()) {
  return ResponseEntity.notFound().build();
}

byte[] data = Files.readAllBytes(Paths.get(resource.getURI()));

return ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
    .body(data);
}
}