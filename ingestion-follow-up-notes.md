# Ingestion Follow-Up Notes

Date: 2026-05-28

Active run observed:
- Project ID: `ab1b088d-7510-440c-b825-a158c49b8a93`
- Process ID: `182d356d-4fc1-4c60-85f1-e1f007d3a70b`
- Log file: `build/logs/knowledge-ingestion-api-name-fallbacks.out.log`

Observed working behavior:
- Service was listening on port `8087`.
- Correct process status endpoint is project-scoped:
  `GET /api/v1/projects/{projectId}/processes`
- Current run progressed from 6/26 to 7/26 files with 0 process-level failures.
- CSV files were detected as `CSV_DOCUMENT` and used `prompt/csv-extraction-prompt.txt`.
- Many XML files were detected as `TIBCO_MDM` and used `prompt/tibco-mdm-extraction-prompt.txt`.
- `documents` and `ingestion_documents` rows should now be created at document start with `PROCESSING`, then updated to `EXTRACTED` or `FAILED`.

Issues to address next:
- Some TIBCO-looking XML files still detected as `GENERIC_XML`, including:
  - `cvAffiliation_MandatoryFields.xml`
  - `fm26ca.xml`
- Deterministic XML DOM extraction failed for several files with:
  `Content is not allowed in prolog.`
  This likely means the parser is receiving Tika text output, a BOM/prefix, or non-raw XML content. The deterministic extractor should parse raw bytes/content before text conversion or strip leading noise safely.
- Enhanced extraction frequently receives LLM JSON that does not match DTO shapes:
  - `KnowledgeDataModel` cannot deserialize because it has no creator/default constructor.
  - `BusinessFlowStep` sometimes arrives as a string instead of an object.
  - Some fields expected as strings arrive as objects or arrays.
  Next action: add DTO creators or a normalization layer that coerces common LLM output shapes before mapping.
- Basic fallback extraction repeats some of the same deserialization failures. It should use the same tolerant parser/normalizer as enhanced extraction.
- Vertex embedding calls hit quota:
  `RESOURCE_EXHAUSTED ... textembedding-gecko`
  Next action: throttle embedding requests, add retry/backoff to embedding service, optionally skip embeddings temporarily while preserving extracted facts.
- As of the 18:52 monitoring pass, embedding quota errors are no longer only log noise; they caused a file-level failure:
  - Process status: `RUNNING`
  - Progress: `8/26`
  - Failed files: `1`
  - Current file: `rbCheckModification.xml`
  - Failure cause: `rbAssignWorkflowParams.xml: Failed to call Vertex AI embedding`
  Next action: do not fail the whole document when embeddings fail. Persist extracted facts first, mark embedding status separately, and retry/defer embedding generation.
- Concurrent ingestion is confirmed by overlapping files in the log:
  - `rbCheckChangedRelationship.xml`
  - `rbCheckModification.xml`
  Both started around 18:51 after `rbAssignWorkflowParams.xml` failed.
- Processing is concurrent but slow on large XML files because each file/chunk can trigger many LLM and embedding calls. Consider limiting per-file chunk LLM calls, batching embeddings, or deferring embeddings to a separate process stage.
- Old killed process rows remain `RUNNING` from earlier forced stops. The UI/API should either show only the active process or mark stale processes as `FAILED`/`ABORTED` during startup cleanup.

Monitoring commands:
```powershell
$projectId = 'ab1b088d-7510-440c-b825-a158c49b8a93'
$processId = '182d356d-4fc1-4c60-85f1-e1f007d3a70b'
$processes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/projects/$projectId/processes" -Method Get -TimeoutSec 30
$processes | Where-Object { "$($_.processId)" -eq $processId } | ConvertTo-Json -Depth 20
```

```powershell
Select-String -Path build\logs\knowledge-ingestion-api-name-fallbacks.out.log -Pattern 'Ingesting project file|Detected document/platform type|Detected TIBCO|No specific platform|Using chunk extraction prompt|Enhanced extraction completed|Failed to ingest|RESOURCE_EXHAUSTED|Exception|ERROR|WARN' | Select-Object -Last 120
```

Fixes applied before restart:
- Stopped the Java process on port `8087` before patching.
- `KnowledgeIngestionService` now reads raw GCS bytes once and passes raw XML text into deterministic XML extraction. Tika text output is still used for chunking and LLM analysis.
- Chunk embedding failures are now skipped with a warning and `null` embedding instead of failing the whole document.
- `DeterministicXmlExtractionService` now strips BOM/leading noise before the first `<`, covering the observed `Content is not allowed in prolog` failure mode.
- Builder-only DTOs used by extraction now include no-args/all-args constructors for Jackson:
  - `KnowledgeDataModel`
  - `KnowledgeDataField`
  - `KnowledgeAPI`
  - `KnowledgeWorkflow`
  - `KnowledgeWorkflowStep`
  - `KnowledgeBusinessRule`
  - `KnowledgeComponent`
  - `KnowledgeIntegration`
  - `KnowledgeResource`
- `BusinessFlowStep` can now deserialize from a scalar string, e.g. `"Review"`.
- `EnhancedKnowledgeExtractionService` now routes both enhanced and fallback extraction through the same tolerant parser and normalizes common LLM shape mismatches:
  - object/array values in string fields are converted to compact strings
  - string workflow steps are converted to `{ "stepName": "..." }`

Verification after fixes:
- `.\gradlew.bat compileJava` passed.
- Focused parser/detector/extraction tests passed.
- `.\gradlew.bat spotlessApply test` passed.
- Port `8087` was not listening after the stop, so the service remained stopped pending restart.

Remaining risks:
- Mitigated: embedding quota exhaustion now has a central fail-open cooldown in `EmbeddingService`.
  - When Vertex returns `RESOURCE_EXHAUSTED` or `Quota exceeded`, embedding generation returns `null` and activates a shared cooldown.
  - Subsequent chunk/entity/project embedding calls skip Vertex during the cooldown instead of repeatedly consuming quota and logging failures.
  - Default cooldown: `knowledge-engine.embedding.quota-cooldown-ms:600000`.
  - `VertexAIService` no longer retries `RESOURCE_EXHAUSTED`, so quota exhaustion fails fast instead of retrying the same exhausted model.
  - Non-quota embedding errors still propagate so configuration/auth/connectivity failures are not silently hidden.
- Mitigated: deterministic XML extraction no longer assumes UTF-8 raw XML bytes. `KnowledgeIngestionService` now detects XML charset from BOM, UTF-16 byte patterns, or the XML declaration `encoding="..."` before decoding raw content. `DeterministicXmlExtractionService` parses the decoded Java string with a character stream so the XML declaration cannot conflict with UTF-8 re-encoding.
- Some TIBCO MDM files may still classify as `GENERIC_XML`; rerun ingestion after restart and record any remaining filenames for detector heuristic improvements.

Verification for XML charset mitigation:
- Added regression coverage for `ISO-8859-1` XML declarations.
- Added regression coverage for UTF-16LE XML byte patterns.
- `.\gradlew.bat spotlessApply test --tests com.kengine.knowledge.ingestion.service.KnowledgeIngestionServiceTest --tests com.kengine.knowledge.ingestion.service.DeterministicXmlExtractionServiceTest` passed.
- `.\gradlew.bat test` passed.

Verification for embedding quota mitigation:
- Added `EmbeddingServiceTest` coverage for quota cooldown activation, cooldown skip behavior, successful embedding conversion, and non-quota error propagation.
- `.\gradlew.bat spotlessApply test --tests com.kengine.knowledge.ingestion.service.ai.EmbeddingServiceTest` passed.
- `.\gradlew.bat test` passed.

Retest after restart:
- Stopped the service, restarted on port `8087`, and started process `a87f3870-7426-4139-9b0c-5d9150bc59dd`.
- Quota mitigation worked: logs showed `Embedding quota exhausted. Skipping embedding generation for 600000 ms`, and ingestion continued.
- New blocking issue found: `affiliation-rules.csv` failed because AI returned a data field with `fieldName = null`, causing `knowledge.knowledge_data_fields.field_name` not-null violation.

Fix applied for data field persistence:
- `KnowledgeGraphService` now supplies required `KnowledgeDataField` values before persistence:
  - `fieldName`: existing field name, then description, then `Field N in <modelName>`
  - `fieldType`: existing field type, then `unknown`
  - `fieldName` is bounded to the DB limit of 255 characters.
- Added `KnowledgeGraphServiceTest` to cover the failed CSV shape.
- `.\gradlew.bat spotlessApply test --tests com.kengine.knowledge.ingestion.service.KnowledgeGraphServiceTest --tests com.kengine.knowledge.ingestion.service.ai.EmbeddingServiceTest` passed.
- `.\gradlew.bat test` passed.

Retest after data-field fix:
- Restarted service on port `8087` under PID `31796`.
- Started process `a8635a8e-89ae-45fd-b9af-7407228d1f9c`.
- As of 19:53, status was `RUNNING`, `8/26`, `0` failed, current file `rbCheckChangedRelationship.xml`.
- The earlier CSV data-field failure did not recur.
- Follow-up quality issues still visible in logs:
  - Some `KnowledgeDataModel` responses still use unexpected shapes/aliases like `name`, `entityName`, and `businessName`.
  - Vertex DNS resolution warnings appeared for `us-central1-aiplatform.googleapis.com`, but the process continued.

Fix applied for `cvAffiliation_MandatoryFields.xml` and `fm26ca.xml` XML classification:
- Cause: the document-level detector was checking Tika/parser text, not the raw XML. Those files contain `Rulebase` tags, but the parsed text used for document-level analysis did not preserve enough XML structure for the TIBCO MDM heuristic.
- `KnowledgeIngestionService` now passes decoded raw XML into `DocumentLevelAnalysisService`.
- `DocumentLevelAnalysisService` now prefers raw XML for platform detection on XML-like files, while continuing to use parsed text for the LLM document analysis prompt.
- Added detector coverage proving `cvAffiliation_MandatoryFields.xml` and `fm26ca.xml` with `Rulebase` content classify as `TIBCO_MDM`.
- Added document-level coverage proving raw XML is used instead of parsed text for XML platform detection.
- `.\gradlew.bat spotlessApply test --tests com.kengine.knowledge.ingestion.service.XMLPlatformDetectorTest --tests com.kengine.knowledge.ingestion.service.DocumentLevelAnalysisServiceTest` passed.
- `.\gradlew.bat test` passed.

Retest after raw XML detection fix:
- Restarted service on port `8087` under PID `30860`.
- Started process `07d15aab-71eb-425e-af35-372ba6d30081`.
- Confirmed in `build/logs/knowledge-ingestion-retest-xml-raw.out.log`:
  - `cvAffiliation_MandatoryFields.xml` detected as `TIBCO_MDM` at document-level analysis.
  - `fm26ca.xml` detected as `TIBCO_MDM` at document-level analysis.
- Separate issue observed during this retest:
  - `affiliation-rules.csv` failed with PostgreSQL `SQLSTATE(08006)` / `An I/O error occurred while sending to the backend`.
  - This is a database connection/resource failure, not a data-field constraint failure and not related to XML detection.
