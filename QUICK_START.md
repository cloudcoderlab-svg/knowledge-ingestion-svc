# Quick Start Guide

## Running the Service with Dev Profile

### Step 1: Set Environment Variables
```bash
export SPRING_PROFILES_ACTIVE=dev
export KENGINE_DB_PASSWORD=Admin@123
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### Step 2: Run the Service
```bash
java -jar build/libs/knowledge-ingestion-svc-1.0.0.jar
```

## Key Information

### Service Port
```
http://localhost:8086
```

### Folder Structure
```
GCS Bucket: kengine-knowledge-artifacts
  staged/<project_id>/<ISO_TIMESTAMP>/     ← Upload files here
  processed/<project_id>/<ISO_TIMESTAMP>/  ← Processed files moved here
```

### Schedulers
```
File Processing:  Every 5 minutes (cron: "0 */5 * * * *")
Folder Creation:  Daily at 2:00 AM (cron: "0 0 2 * * *")
```

**Customize Schedules:**
```bash
export FILE_PROCESSING_CRON="0 */10 * * * *"  # Every 10 minutes
export FOLDER_CREATION_CRON="0 0 3 * * *"     # 3:00 AM daily
```

### Project Discovery
Projects are automatically fetched from `knowledge.projects` table in the database.

### Adding New Projects
```sql
INSERT INTO knowledge.projects (project_id, project_name, source_bucket, gcs_prefix, created_at, updated_at)
VALUES ('my-project', 'My Project', 'kengine-knowledge-artifacts', 'my-project/', NOW(), NOW());
```

### API Endpoints
```
POST /api/v1/projects/{projectId}/folders
POST /api/v1/projects/{projectId}/timestamped-folders
GET  /api/v1/projects/{projectId}/timestamped-folders
GET  /api/v1/projects/{projectId}/timestamped-folders/{timestamp}/files
```

### Uploading Files for Processing
```bash
# 1. Create timestamped folder (or wait for daily scheduler)
curl -X POST http://localhost:8086/api/v1/projects/my-project/timestamped-folders

# 2. Upload files to GCS
gsutil cp document.pdf gs://kengine-knowledge-artifacts/staged/my-project/2026-05-23T10-00-00/

# 3. Wait up to 5 minutes for automatic processing

# 4. Check processed folder
gsutil ls gs://kengine-knowledge-artifacts/processed/my-project/2026-05-23T10-00-00/
```

## Detailed Documentation
- `DEV_PROFILE_SETUP.md` - Complete dev setup guide
- `SCHEDULER_DESIGN.md` - Architecture and design
- `CHANGES_SUMMARY.md` - Full changelog
- `SOLUTION_CAPABILITIES.md` - All capabilities

## Build Status
✅ BUILD SUCCESSFUL
