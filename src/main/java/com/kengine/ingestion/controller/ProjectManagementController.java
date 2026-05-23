package com.kengine.ingestion.controller;

import com.kengine.ingestion.service.GcsFolderManagementService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectManagementController {

  private final GcsFolderManagementService folderManagementService;

  /**
   * Creates a new project folder in the staged directory.
   *
   * @param projectId Project identifier
   * @return Success response
   */
  @PostMapping("/{projectId}/folders")
  public ResponseEntity<Map<String, String>> createProjectFolder(@PathVariable String projectId) {
    log.info("Creating project folder for: {}", projectId);
    folderManagementService.createProjectFolder(projectId);
    return ResponseEntity.ok(
        Map.of(
            "message",
            "Project folder created successfully",
            "projectId",
            projectId,
            "stagedPath",
            "staged/" + projectId));
  }

  /**
   * Gets all files from today's daily dated folder in staged directory for processing.
   *
   * @param projectId Project identifier
   * @return List of file paths from today's staged folder
   */
  @GetMapping("/{projectId}/today/staged-files")
  public ResponseEntity<Map<String, Object>> getTodaysStagedFiles(@PathVariable String projectId) {
    log.info("Getting today's staged files for processing: {}", projectId);
    List<String> files = folderManagementService.getTodaysFilesForProcessing(projectId);
    return ResponseEntity.ok(
        Map.of("projectId", projectId, "stagedFiles", files, "count", files.size()));
  }

  /**
   * Gets all files from today's daily dated folder in processed directory.
   *
   * @param projectId Project identifier
   * @return List of file paths from today's processed folder
   */
  @GetMapping("/{projectId}/today/processed-files")
  public ResponseEntity<Map<String, Object>> getTodaysProcessedFiles(
      @PathVariable String projectId) {
    log.info("Getting today's processed files: {}", projectId);
    List<String> files = folderManagementService.getTodaysProcessedFiles(projectId);
    return ResponseEntity.ok(
        Map.of("projectId", projectId, "processedFiles", files, "count", files.size()));
  }

  /**
   * Creates daily dated folders (yyyy-MM-dd format) for a project in both staged and processed
   * directories. Only creates if they don't already exist.
   *
   * @param projectId Project identifier
   * @return Success response with date
   */
  @PostMapping("/{projectId}/daily-folders")
  public ResponseEntity<Map<String, String>> createDailyDatedFolders(
      @PathVariable String projectId) {
    log.info("Creating daily dated folders for project: {}", projectId);
    String dateFolder = folderManagementService.createDailyDatedFolders(projectId);
    return ResponseEntity.ok(
        Map.of(
            "message",
            "Daily dated folders created successfully",
            "projectId",
            projectId,
            "date",
            dateFolder,
            "stagedPath",
            "staged/" + projectId + "/" + dateFolder,
            "processedPath",
            "processed/" + projectId + "/" + dateFolder));
  }

  /**
   * Checks if daily dated folders exist for today for a project.
   *
   * @param projectId Project identifier
   * @return Existence status
   */
  @GetMapping("/{projectId}/daily-folders/exists")
  public ResponseEntity<Map<String, Object>> checkDailyDatedFoldersExist(
      @PathVariable String projectId) {
    log.info("Checking if daily dated folders exist for project: {}", projectId);
    boolean exists = folderManagementService.dailyDatedFoldersExist(projectId);
    return ResponseEntity.ok(Map.of("projectId", projectId, "exists", exists));
  }
}
