package com.kengine.ingestion.service;

import com.kengine.ingestion.entity.SubjectEntity;
import com.kengine.ingestion.entity.SubjectStatus;
import com.kengine.ingestion.repository.SubjectRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for cleaning up stale subject versions.
 *
 * <p>Automatically removes non-ACTIVE subject versions that are older than 24 hours, keeping only
 * the latest version of each subject regardless of status.
 *
 * <p>Cleanup Rules:
 *
 * <ul>
 *   <li>Latest version (by updated_at) is ALWAYS kept, regardless of status
 *   <li>Older versions with status ACTIVE are kept
 *   <li>Older versions with status DRAFT, INGESTING, or FAILED are deleted if older than 24 hours
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectVersionCleanupService {

  private final SubjectRepository subjectRepository;

  // Retention period in hours for non-ACTIVE versions
  private static final int RETENTION_HOURS = 24;

  /**
   * Scheduled task to clean up stale subject versions.
   *
   * <p>Runs every 12 hours to remove non-ACTIVE versions older than 24 hours, while preserving the
   * latest version of each subject.
   */
  @Scheduled(fixedRate = 43200000) // Run every 12 hours (43200000 ms)
  @Transactional
  public void cleanupStaleVersions() {
    log.info("Starting cleanup of stale subject versions older than {} hours", RETENTION_HOURS);

    try {
      OffsetDateTime cutoffTime = OffsetDateTime.now().minusHours(RETENTION_HOURS);
      int deletedCount = 0;

      // Get all subjects and group by subject name
      List<SubjectEntity> allSubjects = subjectRepository.findAll();
      Map<String, List<SubjectEntity>> subjectsByName =
          allSubjects.stream().collect(Collectors.groupingBy(SubjectEntity::getSubjectName));

      log.info("Found {} unique subjects to analyze", subjectsByName.size());

      // Process each subject name
      for (Map.Entry<String, List<SubjectEntity>> entry : subjectsByName.entrySet()) {
        String subjectName = entry.getKey();
        List<SubjectEntity> versions = entry.getValue();

        if (versions.size() <= 1) {
          // Only one version, skip
          continue;
        }

        // Sort by updated_at descending to find latest
        versions.sort(
            (a, b) -> {
              OffsetDateTime aTime = a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt();
              OffsetDateTime bTime = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
              return bTime.compareTo(aTime); // Descending order
            });

        SubjectEntity latestVersion = versions.get(0);
        log.debug(
            "Subject '{}': Found {} versions, latest is v{} (status: {})",
            subjectName,
            versions.size(),
            latestVersion.getVersion(),
            latestVersion.getStatus());

        // Process older versions
        for (int i = 1; i < versions.size(); i++) {
          SubjectEntity olderVersion = versions.get(i);
          UUID versionId = olderVersion.getSubjectId();
          OffsetDateTime versionTime =
              olderVersion.getUpdatedAt() != null
                  ? olderVersion.getUpdatedAt()
                  : olderVersion.getCreatedAt();

          // Keep ACTIVE versions regardless of age
          if (olderVersion.getStatus() == SubjectStatus.ACTIVE) {
            log.debug(
                "Keeping ACTIVE version: {} v{} (id: {})",
                subjectName,
                olderVersion.getVersion(),
                versionId);
            continue;
          }

          // Delete non-ACTIVE versions older than retention period
          if (versionTime != null && versionTime.isBefore(cutoffTime)) {
            log.info(
                "Deleting stale version: {} v{} (status: {}, age: {} hours, id: {})",
                subjectName,
                olderVersion.getVersion(),
                olderVersion.getStatus(),
                java.time.Duration.between(versionTime, OffsetDateTime.now()).toHours(),
                versionId);

            subjectRepository.delete(olderVersion);
            deletedCount++;
          } else {
            log.debug(
                "Keeping recent non-ACTIVE version: {} v{} (status: {}, id: {})",
                subjectName,
                olderVersion.getVersion(),
                olderVersion.getStatus(),
                versionId);
          }
        }
      }

      log.info(
          "Cleanup completed: Deleted {} stale subject version(s) older than {} hours",
          deletedCount,
          RETENTION_HOURS);

    } catch (Exception e) {
      log.error("Error during subject version cleanup: {}", e.getMessage(), e);
    }
  }
}
