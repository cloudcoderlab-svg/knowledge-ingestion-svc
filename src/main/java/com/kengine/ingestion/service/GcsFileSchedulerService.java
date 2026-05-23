package com.kengine.ingestion.service;

import com.kengine.ingestion.entity.ProjectEntity;
import com.kengine.ingestion.repository.ProjectRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsFileSchedulerService {

  private final GcsFolderManagementService folderManagementService;
  private final KnowledgeIngestionService ingestionService;
  private final ProjectRepository projectRepository;

  // Thread pool for processing files asynchronously
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Value("${scheduler.enabled:true}")
  private boolean schedulerEnabled;

  @PostConstruct
  public void initialize() {
    // Ensure default project folder exists
    folderManagementService.ensureDefaultProjectExists();
    log.info("GCS File Scheduler Service initialized. Scheduler enabled: {}", schedulerEnabled);
  }

  /**
   * Scheduled task that runs every 5 minutes to process files from staged folders. For each
   * project, it: 1. Lists all timestamped folders 2. For each timestamped folder, lists all files
   * 3. Submits each file to the processing queue 4. Moves successfully processed files to the
   * processed folder
   *
   * <p>Cron expression: Every 5 minutes
   */
  @Scheduled(cron = "${scheduler.file-processing.cron:0 */5 * * * *}")
  public void processFilesFromStagedFolders() {
    if (!schedulerEnabled) {
      log.debug("Scheduler is disabled. Skipping file processing.");
      return;
    }

    log.info("Starting scheduled file processing from staged folders");

    // Get list of projects to process
    List<String> projects = getProjectsToProcess();

    for (String projectId : projects) {
      processProjectFiles(projectId);
    }

    log.info("Completed scheduled file processing");
  }

  /**
   * Scheduled task that runs once per day to create timestamped folders for projects. This ensures
   * folders are ready for file uploads.
   *
   * <p>Cron expression: Daily at 2:00 AM. Can be overridden via scheduler.folder-creation.cron
   * property.
   */
  @Scheduled(cron = "${scheduler.folder-creation.cron:0 0 2 * * *}")
  public void createTimestampedFoldersForProjects() {
    if (!schedulerEnabled) {
      log.debug("Scheduler is disabled. Skipping folder creation.");
      return;
    }

    log.info("Creating timestamped folders for projects");

    List<String> projects = getProjectsToProcess();

    for (String projectId : projects) {
      try {
        String timestamp = folderManagementService.createTimestampedFolders(projectId);
        log.info("Created timestamped folders for project {}: {}", projectId, timestamp);
      } catch (Exception e) {
        log.error("Error creating timestamped folders for project {}", projectId, e);
      }
    }

    log.info("Completed timestamped folder creation");
  }

  private void processProjectFiles(String projectId) {
    try {
      List<String> timestampedFolders = folderManagementService.listTimestampedFolders(projectId);

      log.info(
          "Processing {} timestamped folders for project: {}",
          timestampedFolders.size(),
          projectId);

      for (String timestamp : timestampedFolders) {
        processTimestampedFolder(projectId, timestamp);
      }
    } catch (Exception e) {
      log.error("Error processing files for project: {}", projectId, e);
    }
  }

  private void processTimestampedFolder(String projectId, String timestamp) {
    try {
      List<String> files = folderManagementService.listFilesInStagedFolder(projectId, timestamp);

      log.info("Found {} files in staged/{}/{} for processing", files.size(), projectId, timestamp);

      for (String blobName : files) {
        // Submit each file to the processing queue
        executorService.submit(() -> processFile(blobName, projectId, timestamp));
      }
    } catch (Exception e) {
      log.error("Error processing timestamped folder {}/{}", projectId, timestamp, e);
    }
  }

  private void processFile(String blobName, String projectId, String timestamp) {
    try {
      log.info("Processing file: {}", blobName);
      String bucketName = folderManagementService.getBucketName();

      // Process the file using existing ingestion service
      ingestionService.ingestFromGcs(bucketName, blobName);

      // Move to processed folder after successful processing
      String processedBlobName =
          folderManagementService.moveToProcessed(blobName, projectId, timestamp);

      log.info("Successfully processed and moved file: {} -> {}", blobName, processedBlobName);
    } catch (Exception e) {
      log.error("Error processing file: {}", blobName, e);
      // File remains in staged folder for retry on next run
    }
  }

  /**
   * Retrieves all projects from the database to process. If no projects are found, returns the
   * default project.
   */
  private List<String> getProjectsToProcess() {
    try {
      List<ProjectEntity> projects = projectRepository.findAll();
      if (projects.isEmpty()) {
        log.info("No projects found in database. Using default project.");
        return List.of("default-project");
      }

      List<String> projectIds =
          projects.stream().map(ProjectEntity::getProjectId).collect(Collectors.toList());

      log.debug("Found {} projects to process: {}", projectIds.size(), projectIds);
      return projectIds;
    } catch (Exception e) {
      log.error("Error fetching projects from database. Using default project.", e);
      return List.of("default-project");
    }
  }

  public void shutdown() {
    log.info("Shutting down file processing executor service");
    executorService.shutdown();
  }
}
