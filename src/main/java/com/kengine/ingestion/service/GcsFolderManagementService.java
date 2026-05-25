package com.kengine.ingestion.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsFolderManagementService {

  private final Storage storage;

  @Value("${gcp.storage.bucket-name}")
  private String bucketName;

  private static final String SUBJECTS_PREFIX = "subjects/";

  private void createMarkerFile(String path) {
    BlobId blobId = BlobId.of(bucketName, path);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();

    // Check if already exists
    Blob existing = storage.get(blobId);
    if (existing != null) {
      log.debug("Marker file already exists: {}", path);
      return;
    }

    try {
      storage.createFrom(blobInfo, new ByteArrayInputStream(new byte[0]));
      log.debug("Created marker file: {}", path);
    } catch (Exception e) {
      log.error("Error creating marker file: {}", path, e);
      throw new RuntimeException("Failed to create marker file: " + path, e);
    }
  }

  private String extractFileName(String fullPath) {
    int lastSlash = fullPath.lastIndexOf('/');
    return lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
  }

  public String getBucketName() {
    return bucketName;
  }

  // ============================================================================
  // Subject-based GCS Structure Methods
  // ============================================================================

  /**
   * Creates a subject folder in GCS.
   *
   * @param subjectName The subject name (used as folder name)
   * @return The GCS folder URL
   */
  public String createSubjectFolder(String subjectName) {
    String folderPath = SUBJECTS_PREFIX + subjectName + "/.folder";
    createMarkerFile(folderPath);
    log.info("Created subject folder for: {}", subjectName);
    return "gs://" + bucketName + "/" + SUBJECTS_PREFIX + subjectName + "/";
  }

  /**
   * Creates a definition.md file in the subject folder with context information.
   *
   * @param subjectName The subject name
   * @param title The subject title
   * @param description The subject description
   * @param subjectId The subject UUID
   * @return The GCS path to the definition.md file
   */
  public String createDefinitionFile(
      String subjectName, String title, String description, String subjectId) {
    String definitionPath = SUBJECTS_PREFIX + subjectName + "/definition.md";

    String content =
        String.format(
            """
            # %s

            ## Description
            %s

            ## Purpose
            This file provides context for AI-powered semantic chunking and knowledge extraction.

            ---
            *Auto-generated - Subject ID: %s*
            """,
            title, description, subjectId);

    BlobId blobId = BlobId.of(bucketName, definitionPath);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/markdown").build();

    try {
      storage.createFrom(blobInfo, new ByteArrayInputStream(content.getBytes()));
      log.info("Created definition.md for subject: {}", subjectName);
      return definitionPath;
    } catch (Exception e) {
      log.error("Error creating definition.md for subject: {}", subjectName, e);
      throw new RuntimeException("Failed to create definition.md", e);
    }
  }

  /**
   * Lists all files in a subject folder, excluding definition.md and marker files.
   *
   * @param subjectName The subject name
   * @return List of file paths
   */
  public List<String> listSubjectFiles(String subjectName) {
    String prefix = SUBJECTS_PREFIX + subjectName + "/";
    List<String> files = new ArrayList<>();

    storage
        .list(bucketName, BlobListOption.prefix(prefix))
        .iterateAll()
        .forEach(
            blob -> {
              String name = blob.getName();
              // Exclude definition.md, marker files, and directory markers
              if (!name.endsWith("/definition.md")
                  && !name.endsWith("/.folder")
                  && !name.endsWith("/")
                  && !name.equals(prefix)) {
                files.add(name);
              }
            });

    log.debug("Found {} files in subject folder: {}", files.size(), subjectName);
    return files;
  }

  /**
   * Reads the definition.md content from a subject folder.
   *
   * @param subjectName The subject name
   * @return The content of definition.md, or null if not found
   */
  public String readDefinitionFile(String subjectName) {
    String definitionPath = SUBJECTS_PREFIX + subjectName + "/definition.md";
    BlobId blobId = BlobId.of(bucketName, definitionPath);
    Blob blob = storage.get(blobId);

    if (blob == null) {
      log.warn("definition.md not found for subject: {}", subjectName);
      return null;
    }

    try {
      byte[] content = blob.getContent();
      return new String(content);
    } catch (Exception e) {
      log.error("Error reading definition.md for subject: {}", subjectName, e);
      return null;
    }
  }

  /**
   * Extracts subject name from a GCS object path.
   *
   * @param objectPath Full GCS object path (e.g., subjects/healthcare-system/doc.pdf)
   * @return Subject name or null if not in subjects folder
   */
  public String extractSubjectNameFromPath(String objectPath) {
    if (!objectPath.startsWith(SUBJECTS_PREFIX)) {
      return null;
    }

    String pathWithoutPrefix = objectPath.substring(SUBJECTS_PREFIX.length());
    int firstSlash = pathWithoutPrefix.indexOf('/');
    return firstSlash > 0 ? pathWithoutPrefix.substring(0, firstSlash) : pathWithoutPrefix;
  }
}
