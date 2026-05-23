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

    // Create today's dated folders for all projects on startup
    createDailyFoldersForAllProjects();

    log.info("GCS File Scheduler Service initialized. Scheduler enabled: {}", schedulerEnabled);
  }

  /**
   * Scheduled task that runs every 5 minutes to process files from today's daily dated staged
   * folder. For each project, it: 1. Gets all files from today's dated folder 2. Submits each file
   * to the processing queue 3. Moves successfully processed files to the processed folder
   *
   * <p>Cron expression: Every 5 minutes
   */
  @Scheduled(cron = "${scheduler.file-processing.cron:0 */5 * * * *}")
  public void processFilesFromStagedFolders() {
    if (!schedulerEnabled) {
      log.debug("Scheduler is disabled. Skipping file processing.");
      return;
    }

    log.info("Starting scheduled file processing from today's daily dated folders");

    List<String> projects = getProjectsToProcess();

    for (String projectId : projects) {
      processTodaysDailyDatedFolder(projectId);
    }

    log.info("Completed scheduled file processing");
  }

  /**
   * Scheduled task that runs every 15 minutes to ensure daily dated folders exist for all projects.
   * Creates folders in ISO date format (yyyy-MM-dd) if they don't already exist.
   *
   * <p>Cron expression: Every 15 minutes
   */
  @Scheduled(cron = "0 */15 * * * *")
  public void ensureDailyDatedFoldersExist() {
    if (!schedulerEnabled) {
      log.debug("Scheduler is disabled. Skipping daily folder check.");
      return;
    }

    log.info("Checking/creating daily dated folders for all projects");
    createDailyFoldersForAllProjects();
    log.info("Completed daily dated folder check");
  }

  /**
   * Processes files from today's daily dated folder for a project. Retrieves all files from today's
   * dated folder and submits them for processing.
   */
  private void processTodaysDailyDatedFolder(String projectId) {
    try {
      List<String> files = folderManagementService.getTodaysFilesForProcessing(projectId);

      log.info("Found {} files in today's dated folder for project: {}", files.size(), projectId);

      // Get today's date folder for moving files
      String today =
          java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
              .withZone(java.time.ZoneOffset.UTC)
              .format(java.time.Instant.now());

      for (String blobName : files) {
        // Submit each file to the processing queue
        executorService.submit(() -> processFile(blobName, projectId, today));
      }
    } catch (Exception e) {
      log.error("Error processing today's dated folder for project: {}", projectId, e);
    }
  }

  private void processFile(String blobName, String projectId, String dateFolder) {
    try {
      log.info("Processing file: {}", blobName);
      String bucketName = folderManagementService.getBucketName();

      // Process the file using existing ingestion service
      ingestionService.ingestFromGcs(bucketName, blobName);

      // Move to processed folder after successful processing
      String processedBlobName =
          folderManagementService.moveToProcessed(blobName, projectId, dateFolder);

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

      List<String> projectIds =
          projects.stream().map(ProjectEntity::getProjectId).collect(Collectors.toList());

      // Always include default-project if it's not already in the list
      if (!projectIds.contains("default-project")) {
        projectIds.add("default-project");
        log.debug("Added default-project to processing list");
      }

      if (projectIds.isEmpty()) {
        log.info("No projects found in database. Using default project.");
        return List.of("default-project");
      }

      log.debug("Found {} projects to process: {}", projectIds.size(), projectIds);
      return projectIds;
    } catch (Exception e) {
      log.error("Error fetching projects from database. Using default project.", e);
      return List.of("default-project");
    }
  }

  /**
   * Creates daily dated folders (yyyy-MM-dd format) for all projects. Checks if folders already
   * exist before creating to avoid duplicates.
   */
  private void createDailyFoldersForAllProjects() {
    List<String> projects = getProjectsToProcess();

    for (String projectId : projects) {
      try {
        String dateFolder = folderManagementService.createDailyDatedFolders(projectId);
        log.info("Ensured daily dated folders exist for project {}: {}", projectId, dateFolder);
      } catch (Exception e) {
        log.error("Error creating daily dated folders for project {}", projectId, e);
      }
    }
  }

  public void shutdown() {
    log.info("Shutting down file processing executor service");
    executorService.shutdown();
  }
}
