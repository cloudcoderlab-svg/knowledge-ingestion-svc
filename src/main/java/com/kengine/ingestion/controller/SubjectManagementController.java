package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.CreateSubjectRequest;
import com.kengine.ingestion.dto.SignedUrlRequest;
import com.kengine.ingestion.dto.SignedUrlResponse;
import com.kengine.ingestion.dto.SubjectResponse;
import com.kengine.ingestion.service.SubjectManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectManagementController {

  private final SubjectManagementService subjectManagementService;

  /**
   * Creates a new subject.
   *
   * @param request Create subject request
   * @return Subject response
   */
  @PostMapping
  public ResponseEntity<SubjectResponse> createSubject(
      @Valid @RequestBody CreateSubjectRequest request) {
    log.info("Creating subject with title: {}", request.getTitle());
    SubjectResponse response = subjectManagementService.createSubject(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Gets a subject by ID.
   *
   * @param subjectId Subject UUID
   * @return Subject response
   */
  @GetMapping("/{subjectId}")
  public ResponseEntity<SubjectResponse> getSubject(@PathVariable UUID subjectId) {
    log.info("Getting subject: {}", subjectId);
    SubjectResponse response = subjectManagementService.getSubject(subjectId);
    return ResponseEntity.ok(response);
  }

  /**
   * Lists all subjects.
   *
   * @return List of subject responses
   */
  @GetMapping
  public ResponseEntity<List<SubjectResponse>> listSubjects() {
    log.info("Listing all subjects");
    List<SubjectResponse> response = subjectManagementService.listSubjects();
    return ResponseEntity.ok(response);
  }

  /**
   * Generates a signed URL for uploading a file to the subject folder.
   *
   * @param subjectId Subject UUID
   * @param request Signed URL request
   * @return Signed URL response
   */
  @PostMapping("/{subjectId}/upload-url")
  public ResponseEntity<SignedUrlResponse> generateUploadUrl(
      @PathVariable UUID subjectId, @Valid @RequestBody SignedUrlRequest request) {
    log.info("Generating upload URL for subject: {}, file: {}", subjectId, request.getFileName());
    SignedUrlResponse response =
        subjectManagementService.generateUploadUrl(
            subjectId,
            request.getFileName(),
            request.getContentType(),
            request.getExpirationMinutes());
    return ResponseEntity.ok(response);
  }

  /**
   * Lists files in a subject folder.
   *
   * @param subjectId Subject UUID
   * @return List of file paths
   */
  @GetMapping("/{subjectId}/files")
  public ResponseEntity<List<String>> listSubjectFiles(@PathVariable UUID subjectId) {
    log.info("Listing files for subject: {}", subjectId);
    List<String> files = subjectManagementService.listSubjectFiles(subjectId);
    return ResponseEntity.ok(files);
  }
}
