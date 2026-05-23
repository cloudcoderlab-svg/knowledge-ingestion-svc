package com.kengine.ingestion.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

  private static final String STAGED_PREFIX = "staged/";
  private static final String PROCESSED_PREFIX = "processed/";
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

  /**
   * Creates project-specific folders in both staged and processed directories. Creates marker files
   * to ensure folders exist in GCS.
   */
  public void createProjectFolder(String projectId) {
    String stagedFolderPath = STAGED_PREFIX + projectId + "/.folder";
    String processedFolderPath = PROCESSED_PREFIX + projectId + "/.folder";

    createMarkerFile(stagedFolderPath);
    createMarkerFile(processedFolderPath);

    log.info("Created project folders (staged and processed) for project: {}", projectId);
  }

  /**
   * Creates timestamped folders for a project in both staged and processed directories. Returns the
   * timestamp used for folder naming.
   */
  public String createTimestampedFolders(String projectId) {
    String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());

    String stagedFolder = STAGED_PREFIX + projectId + "/" + timestamp + "/.folder";
    String processedFolder = PROCESSED_PREFIX + projectId + "/" + timestamp + "/.folder";

    createMarkerFile(stagedFolder);
    createMarkerFile(processedFolder);

    log.info("Created timestamped folders for project {}: {}", projectId, timestamp);
    return timestamp;
  }

  /**
   * Lists all files in a specific staged folder path (project + timestamp).
   *
   * @param projectId Project identifier
   * @param timestamp ISO timestamp folder name
   * @return List of blob names (full paths)
   */
  public List<String> listFilesInStagedFolder(String projectId, String timestamp) {
    String prefix = STAGED_PREFIX + projectId + "/" + timestamp + "/";
    List<String> files = new ArrayList<>();

    storage
        .list(bucketName, BlobListOption.prefix(prefix))
        .iterateAll()
        .forEach(
            blob -> {
              String name = blob.getName();
              // Exclude marker files and directory markers
              if (!name.endsWith("/.folder") && !name.endsWith("/")) {
                files.add(name);
              }
            });

    log.debug("Found {} files in staged folder: {}", files.size(), prefix);
    return files;
  }

  /**
   * Lists all timestamped folders for a project in the staged directory. Returns folder names
   * (timestamps only, not full paths).
   */
  public List<String> listTimestampedFolders(String projectId) {
    String prefix = STAGED_PREFIX + projectId + "/";
    List<String> folders = new ArrayList<>();

    storage
        .list(bucketName, BlobListOption.prefix(prefix), BlobListOption.currentDirectory())
        .iterateAll()
        .forEach(
            blob -> {
              String name = blob.getName();
              // Extract folder name (timestamp) from path
              if (name.startsWith(prefix) && name.length() > prefix.length()) {
                String folderName = name.substring(prefix.length()).replaceAll("/$", "");
                if (!folderName.isEmpty()) {
                  folders.add(folderName);
                }
              }
            });

    log.debug("Found {} timestamped folders for project: {}", folders.size(), projectId);
    return folders;
  }

  /**
   * Moves a file from staged to processed folder.
   *
   * @param stagedBlobName Full path of the staged file
   * @param projectId Project identifier
   * @param timestamp Timestamp folder name
   * @return The new blob name in processed folder
   */
  public String moveToProcessed(String stagedBlobName, String projectId, String timestamp) {
    Blob stagedBlob = storage.get(BlobId.of(bucketName, stagedBlobName));
    if (stagedBlob == null) {
      log.warn("Staged blob not found: {}", stagedBlobName);
      return null;
    }

    // Extract filename from staged path
    String fileName = extractFileName(stagedBlobName);
    String processedBlobName = PROCESSED_PREFIX + projectId + "/" + timestamp + "/" + fileName;

    // Copy to processed folder
    BlobId processedBlobId = BlobId.of(bucketName, processedBlobName);
    storage.copy(
        Storage.CopyRequest.newBuilder()
            .setSource(bucketName, stagedBlobName)
            .setTarget(processedBlobId)
            .build());

    // Delete from staged
    stagedBlob.delete();

    log.info("Moved file from staged to processed: {} -> {}", stagedBlobName, processedBlobName);
    return processedBlobName;
  }

  /** Creates the default project folder if it doesn't exist. */
  public void ensureDefaultProjectExists() {
    createProjectFolder("default-project");
  }

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
}
