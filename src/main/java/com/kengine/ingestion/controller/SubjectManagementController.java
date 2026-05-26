package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.CreateSubjectRequest;
import com.kengine.ingestion.dto.SignedUrlRequest;
import com.kengine.ingestion.dto.SignedUrlResponse;
import com.kengine.ingestion.dto.SubjectResponse;
import com.kengine.ingestion.service.SubjectManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing subjects.
 *
 * <p>A subject represents a knowledge domain or project. Each subject has its own GCS folder for
 * document storage and a definition.md file describing its scope.
 */
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Subject Management",
    description =
        "APIs for managing subjects, which represent knowledge domains or projects. "
            + "Provides operations for creating subjects, retrieving metadata, managing files, and generating upload URLs.")
public class SubjectManagementController {

  private final SubjectManagementService subjectManagementService;

  /**
   * Creates a new subject.
   *
   * @param request Create subject request
   * @return Subject response
   */
  @Operation(
      summary = "Create a new subject",
      description =
          "Creates a new subject with a dedicated GCS folder and definition.md file. "
              + "A subject represents a knowledge domain or project that contains documents to be ingested.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Subject created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SubjectResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (missing required fields or invalid data)",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during subject creation",
            content = @Content)
      })
  @PostMapping
  public ResponseEntity<SubjectResponse> createSubject(
      @Parameter(
              description = "Subject creation request with title and description",
              required = true)
          @Valid
          @RequestBody
          CreateSubjectRequest request) {
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
  @Operation(
      summary = "Get subject details",
      description =
          "Retrieves detailed information about a specific subject by its unique identifier.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subject retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SubjectResponse.class))),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @GetMapping("/{subjectId}")
  public ResponseEntity<SubjectResponse> getSubject(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId) {
    log.info("Getting subject: {}", subjectId);
    SubjectResponse response = subjectManagementService.getSubject(subjectId);
    return ResponseEntity.ok(response);
  }

  /**
   * Lists all subjects.
   *
   * @return List of subject responses
   */
  @Operation(
      summary = "List all subjects",
      description = "Retrieves a list of all subjects in the system with their metadata.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subjects retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
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
  @Operation(
      summary = "Generate signed upload URL",
      description =
          "Creates a time-limited signed URL for uploading files directly to the subject's GCS folder. "
              + "The URL allows PUT operations for the specified duration.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Signed URL generated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SignedUrlResponse.class))),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content),
        @ApiResponse(
            responseCode = "500",
            description = "Error generating signed URL",
            content = @Content)
      })
  @PostMapping("/{subjectId}/upload-url")
  public ResponseEntity<SignedUrlResponse> generateUploadUrl(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(
              description = "Upload URL request with file details and expiration time",
              required = true)
          @Valid
          @RequestBody
          SignedUrlRequest request) {
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
  @Operation(
      summary = "List files in subject folder",
      description =
          "Retrieves a list of all files stored in the subject's GCS folder. "
              + "Returns the relative file paths within the subject folder.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "File list retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @GetMapping("/{subjectId}/files")
  public ResponseEntity<List<String>> listSubjectFiles(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId) {
    log.info("Listing files for subject: {}", subjectId);
    List<String> files = subjectManagementService.listSubjectFiles(subjectId);
    return ResponseEntity.ok(files);
  }
}
