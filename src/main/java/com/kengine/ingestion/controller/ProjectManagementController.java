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
   * Creates timestamped folders for a project in both staged and processed directories.
   *
   * @param projectId Project identifier
   * @return Success response with timestamp
   */
  @PostMapping("/{projectId}/timestamped-folders")
  public ResponseEntity<Map<String, String>> createTimestampedFolders(
      @PathVariable String projectId) {
    log.info("Creating timestamped folders for project: {}", projectId);
    String timestamp = folderManagementService.createTimestampedFolders(projectId);
    return ResponseEntity.ok(
        Map.of(
            "message",
            "Timestamped folders created successfully",
            "projectId",
            projectId,
            "timestamp",
            timestamp,
            "stagedPath",
            "staged/" + projectId + "/" + timestamp,
            "processedPath",
            "processed/" + projectId + "/" + timestamp));
  }

  /**
   * Lists all timestamped folders for a project.
   *
   * @param projectId Project identifier
   * @return List of timestamp folder names
   */
  @GetMapping("/{projectId}/timestamped-folders")
  public ResponseEntity<Map<String, Object>> listTimestampedFolders(
      @PathVariable String projectId) {
    log.info("Listing timestamped folders for project: {}", projectId);
    List<String> folders = folderManagementService.listTimestampedFolders(projectId);
    return ResponseEntity.ok(
        Map.of("projectId", projectId, "folders", folders, "count", folders.size()));
  }

  /**
   * Lists all files in a specific timestamped staged folder.
   *
   * @param projectId Project identifier
   * @param timestamp Timestamp folder name
   * @return List of file paths
   */
  @GetMapping("/{projectId}/timestamped-folders/{timestamp}/files")
  public ResponseEntity<Map<String, Object>> listFilesInTimestampedFolder(
      @PathVariable String projectId, @PathVariable String timestamp) {
    log.info("Listing files in staged/{}/{}", projectId, timestamp);
    List<String> files = folderManagementService.listFilesInStagedFolder(projectId, timestamp);
    return ResponseEntity.ok(
        Map.of(
            "projectId", projectId,
            "timestamp", timestamp,
            "files", files,
            "count", files.size()));
  }
}
