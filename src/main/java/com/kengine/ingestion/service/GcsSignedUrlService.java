package com.kengine.ingestion.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsSignedUrlService {

  private final Storage storage;

  /**
   * Generates a signed URL for uploading a file to GCS.
   *
   * @param bucket The GCS bucket name
   * @param objectPath The full object path in the bucket
   * @param contentType The content type of the file
   * @param expirationMinutes Expiration time in minutes
   * @return Signed URL as a string
   */
  public String generateUploadUrl(
      String bucket, String objectPath, String contentType, int expirationMinutes) {
    try {
      BlobId blobId = BlobId.of(bucket, objectPath);
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

      URL signedUrl =
          storage.signUrl(
              blobInfo,
              expirationMinutes,
              TimeUnit.MINUTES,
              SignUrlOption.httpMethod(HttpMethod.PUT),
              SignUrlOption.withV4Signature());

      log.info(
          "Generated signed upload URL for object: {} in bucket: {}, expires in {} minutes",
          objectPath,
          bucket,
          expirationMinutes);

      return signedUrl.toString();
    } catch (Exception e) {
      String errorMsg = e.getMessage();
      if (errorMsg != null && errorMsg.contains("Signing key was not provided")) {
        log.error(
            "Failed to generate signed URL for {}/{}: Service account credentials required. "
                + "Set 'gcp.credentials.file' property to path of service account JSON key file, "
                + "or use 'gcloud auth application-default login --impersonate-service-account=SERVICE_ACCOUNT_EMAIL'",
            bucket,
            objectPath);
        throw new RuntimeException(
            "Signed URL generation requires service account credentials. "
                + "Configure 'gcp.credentials.file' property or use gcloud impersonation.",
            e);
      }
      log.error("Failed to generate signed URL for {}/{}: {}", bucket, objectPath, e.getMessage());
      throw new RuntimeException("Failed to generate signed URL", e);
    }
  }

  /**
   * Generates a signed URL for downloading a file from GCS.
   *
   * @param bucket The GCS bucket name
   * @param objectPath The full object path in the bucket
   * @param expirationMinutes Expiration time in minutes
   * @return Signed URL as a string
   */
  public String generateDownloadUrl(String bucket, String objectPath, int expirationMinutes) {
    try {
      BlobId blobId = BlobId.of(bucket, objectPath);
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

      URL signedUrl =
          storage.signUrl(
              blobInfo,
              expirationMinutes,
              TimeUnit.MINUTES,
              SignUrlOption.httpMethod(HttpMethod.GET),
              SignUrlOption.withV4Signature());

      log.info(
          "Generated signed download URL for object: {} in bucket: {}, expires in {} minutes",
          objectPath,
          bucket,
          expirationMinutes);

      return signedUrl.toString();
    } catch (Exception e) {
      log.error("Failed to generate signed URL for {}/{}: {}", bucket, objectPath, e.getMessage());
      throw new RuntimeException("Failed to generate signed URL", e);
    }
  }
}
