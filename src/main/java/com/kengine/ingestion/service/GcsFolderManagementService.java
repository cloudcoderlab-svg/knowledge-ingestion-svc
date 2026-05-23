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
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

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
   * Lists all files in a specific staged folder path (project + date).
   *
   * @param projectId Project identifier
   * @param dateFolder Date folder name (yyyy-MM-dd)
   * @return List of blob names (full paths)
   */
  public List<String> listFilesInStagedFolder(String projectId, String dateFolder) {
    String prefix = STAGED_PREFIX + projectId + "/" + dateFolder + "/";
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
   * Moves a file from staged to processed folder.
   *
   * @param stagedBlobName Full path of the staged file
   * @param projectId Project identifier
   * @param dateFolder Date folder name (yyyy-MM-dd)
   * @return The new blob name in processed folder
   */
  public String moveToProcessed(String stagedBlobName, String projectId, String dateFolder) {
    Blob stagedBlob = storage.get(BlobId.of(bucketName, stagedBlobName));
    if (stagedBlob == null) {
      log.warn("Staged blob not found: {}", stagedBlobName);
      return null;
    }

    // Extract filename from staged path
    String fileName = extractFileName(stagedBlobName);
    String processedBlobName = PROCESSED_PREFIX + projectId + "/" + dateFolder + "/" + fileName;

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

  /**
   * Gets all files from today's daily dated folder in the staged directory for processing.
   *
   * @param projectId Project identifier
   * @return List of file paths from today's staged folder
   */
  public List<String> getTodaysFilesForProcessing(String projectId) {
    String today = DATE_FORMATTER.format(Instant.now());
    List<String> files = listFilesInStagedFolder(projectId, today);

    log.info(
        "Found {} files for processing in staged/{}/{} for today", files.size(), projectId, today);
    return files;
  }

  /**
   * Lists all files in a specific processed folder path (project + date).
   *
   * @param projectId Project identifier
   * @param dateFolder Date folder name (yyyy-MM-dd)
   * @return List of blob names (full paths)
   */
  public List<String> listFilesInProcessedFolder(String projectId, String dateFolder) {
    String prefix = PROCESSED_PREFIX + projectId + "/" + dateFolder + "/";
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

    log.debug("Found {} files in processed folder: {}", files.size(), prefix);
    return files;
  }

  /**
   * Gets all files from today's daily dated folder in the processed directory.
   *
   * @param projectId Project identifier
   * @return List of file paths from today's processed folder
   */
  public List<String> getTodaysProcessedFiles(String projectId) {
    String today = DATE_FORMATTER.format(Instant.now());
    List<String> files = listFilesInProcessedFolder(projectId, today);

    log.info(
        "Found {} processed files in processed/{}/{} for today", files.size(), projectId, today);
    return files;
  }

  /**
   * Creates daily dated folders for a project in both staged and processed directories. Uses
   * ISO-8601 date format (yyyy-MM-dd). Only creates if they don't already exist.
   *
   * @param projectId Project identifier
   * @return The date string used for folder naming (yyyy-MM-dd)
   */
  public String createDailyDatedFolders(String projectId) {
    String today = DATE_FORMATTER.format(Instant.now());

    String stagedFolder = STAGED_PREFIX + projectId + "/" + today + "/.folder";
    String processedFolder = PROCESSED_PREFIX + projectId + "/" + today + "/.folder";

    // Check if folders already exist
    BlobId stagedBlobId = BlobId.of(bucketName, stagedFolder);
    BlobId processedBlobId = BlobId.of(bucketName, processedFolder);

    boolean stagedExists = storage.get(stagedBlobId) != null;
    boolean processedExists = storage.get(processedBlobId) != null;

    if (stagedExists && processedExists) {
      log.debug("Daily dated folders already exist for project {} on {}", projectId, today);
      return today;
    }

    // Create folders if they don't exist
    if (!stagedExists) {
      createMarkerFile(stagedFolder);
    }
    if (!processedExists) {
      createMarkerFile(processedFolder);
    }

    log.info("Created daily dated folders for project {} on {}", projectId, today);
    return today;
  }

  /**
   * Checks if daily dated folders exist for today for a project.
   *
   * @param projectId Project identifier
   * @return true if both staged and processed folders exist for today
   */
  public boolean dailyDatedFoldersExist(String projectId) {
    String today = DATE_FORMATTER.format(Instant.now());
    String stagedFolder = STAGED_PREFIX + projectId + "/" + today + "/.folder";
    String processedFolder = PROCESSED_PREFIX + projectId + "/" + today + "/.folder";

    BlobId stagedBlobId = BlobId.of(bucketName, stagedFolder);
    BlobId processedBlobId = BlobId.of(bucketName, processedFolder);

    boolean exists = storage.get(stagedBlobId) != null && storage.get(processedBlobId) != null;
    log.debug("Daily dated folders exist for project {} on {}: {}", projectId, today, exists);
    return exists;
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
