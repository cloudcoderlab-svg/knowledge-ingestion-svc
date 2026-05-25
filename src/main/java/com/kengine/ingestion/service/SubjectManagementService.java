package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.CreateSubjectRequest;
import com.kengine.ingestion.dto.SignedUrlResponse;
import com.kengine.ingestion.dto.SubjectResponse;
import com.kengine.ingestion.entity.SubjectEntity;
import com.kengine.ingestion.repository.SubjectRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectManagementService {

  private final SubjectRepository subjectRepository;
  private final GcsFolderManagementService gcsFolderManagementService;
  private final GcsSignedUrlService gcsSignedUrlService;

  @Value("${gcp.storage.bucket-name}")
  private String bucketName;

  /**
   * Creates a new subject with GCS folder and definition.md file.
   *
   * @param request Create subject request
   * @return Subject response with GCS details
   */
  @Transactional
  public SubjectResponse createSubject(CreateSubjectRequest request) {
    // Generate subject name from title if not provided, then normalize to kebab-case
    String subjectName =
        request.getSubjectName() != null
            ? normalizeToKebabCase(request.getSubjectName())
            : normalizeToKebabCase(request.getTitle());

    // Check if subject name already exists
    if (subjectRepository.existsBySubjectName(subjectName)) {
      throw new IllegalArgumentException("Subject with name '" + subjectName + "' already exists");
    }

    // Create subject entity
    SubjectEntity subject =
        SubjectEntity.builder()
            .subjectName(subjectName)
            .title(request.getTitle())
            .description(request.getDescription())
            .sourceBucket(bucketName)
            .gcsFolderUrl("") // Will be set after folder creation
            .build();

    // Save to get the generated ID
    subject = subjectRepository.save(subject);

    // Create GCS folder
    String gcsFolderUrl = gcsFolderManagementService.createSubjectFolder(subjectName);
    subject.setGcsFolderUrl(gcsFolderUrl);

    // Create definition.md file
    String definitionPath =
        gcsFolderManagementService.createDefinitionFile(
            subjectName,
            request.getTitle(),
            request.getDescription(),
            subject.getSubjectId().toString());

    // Save updated entity with GCS folder URL
    subject = subjectRepository.save(subject);

    log.info("Created subject: {} with ID: {}", subjectName, subject.getSubjectId());

    // Build response
    return SubjectResponse.builder()
        .subjectId(subject.getSubjectId())
        .subjectName(subject.getSubjectName())
        .title(subject.getTitle())
        .description(subject.getDescription())
        .sourceBucket(subject.getSourceBucket())
        .gcsFolderUrl(subject.getGcsFolderUrl())
        .definitionUrl("gs://" + bucketName + "/" + definitionPath)
        .createdAt(subject.getCreatedAt())
        .updatedAt(subject.getUpdatedAt())
        .build();
  }

  /**
   * Gets a subject by ID.
   *
   * @param subjectId Subject UUID
   * @return Subject response
   */
  public SubjectResponse getSubject(UUID subjectId) {
    SubjectEntity subject =
        subjectRepository
            .findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

    return toSubjectResponse(subject);
  }

  /**
   * Gets a subject by name.
   *
   * @param subjectName Subject name
   * @return Subject response
   */
  public SubjectResponse getSubjectByName(String subjectName) {
    SubjectEntity subject =
        subjectRepository
            .findBySubjectName(subjectName)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectName));

    return toSubjectResponse(subject);
  }

  /**
   * Lists all subjects.
   *
   * @return List of subject responses
   */
  public List<SubjectResponse> listSubjects() {
    return subjectRepository.findAll().stream()
        .map(this::toSubjectResponse)
        .collect(Collectors.toList());
  }

  /**
   * Generates a signed URL for uploading a file to the subject folder.
   *
   * @param subjectId Subject UUID
   * @param fileName File name
   * @param contentType Content type
   * @param expirationMinutes Expiration in minutes
   * @return Signed URL response
   */
  public SignedUrlResponse generateUploadUrl(
      UUID subjectId, String fileName, String contentType, int expirationMinutes) {
    SubjectEntity subject =
        subjectRepository
            .findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

    String objectPath = "subjects/" + subject.getSubjectName() + "/" + fileName;
    String signedUrl =
        gcsSignedUrlService.generateUploadUrl(
            bucketName, objectPath, contentType, expirationMinutes);

    log.info(
        "Generated upload URL for subject: {}, file: {}, expires in {} minutes",
        subject.getSubjectName(),
        fileName,
        expirationMinutes);

    return SignedUrlResponse.builder()
        .signedUrl(signedUrl)
        .fileName(fileName)
        .gcsPath("gs://" + bucketName + "/" + objectPath)
        .expiresInMinutes(expirationMinutes)
        .build();
  }

  /**
   * Lists files in a subject folder.
   *
   * @param subjectId Subject UUID
   * @return List of file paths
   */
  public List<String> listSubjectFiles(UUID subjectId) {
    SubjectEntity subject =
        subjectRepository
            .findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

    return gcsFolderManagementService.listSubjectFiles(subject.getSubjectName());
  }

  private SubjectResponse toSubjectResponse(SubjectEntity subject) {
    return SubjectResponse.builder()
        .subjectId(subject.getSubjectId())
        .subjectName(subject.getSubjectName())
        .title(subject.getTitle())
        .description(subject.getDescription())
        .sourceBucket(subject.getSourceBucket())
        .gcsFolderUrl(subject.getGcsFolderUrl())
        .definitionUrl(
            "gs://" + bucketName + "/subjects/" + subject.getSubjectName() + "/definition.md")
        .createdAt(subject.getCreatedAt())
        .updatedAt(subject.getUpdatedAt())
        .build();
  }

  /**
   * Normalizes a string to kebab-case format (lowercase with hyphens).
   *
   * @param input Input string (title or subject name)
   * @return Normalized kebab-case string
   */
  private String normalizeToKebabCase(String input) {
    if (input == null || input.trim().isEmpty()) {
      throw new IllegalArgumentException("Input cannot be null or empty");
    }

    String normalized =
        input.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");

    // Limit to 100 characters
    if (normalized.length() > 100) {
      normalized = normalized.substring(0, 100);
      // Remove trailing hyphen if substring cut in the middle
      normalized = normalized.replaceAll("-+$", "");
    }

    return normalized;
  }
}
