# Implementation Plan: OpenAPI REST Service

## Overview

This implementation adds formal OpenAPI specifications and automated code generation for all REST endpoints in the filetrans Spring Boot Modulith application. The approach maintains strict module boundaries while introducing type-safe DTOs, mappers, and comprehensive API documentation through Swagger UI.

## Tasks

- [x] 1. Set up OpenAPI code generation infrastructure
  - Add openapi-generator-maven-plugin to pom.xml with three executions (transfer, metadata, ai)
  - Add build-helper-maven-plugin to include generated sources in build path
  - Add springdoc-openapi-starter-webmvc-ui dependency for Swagger UI
  - Add jqwik dependency for property-based testing
  - Configure springdoc in application.yaml (api-docs and swagger-ui paths)
  - _Requirements: 2.1, 2.2, 2.3, 7.1, 7.2, 11.1, 11.2_

- [x] 2. Create OpenAPI specification for Transfer module
  - [x] 2.1 Create src/main/resources/openapi/transfer-api.yaml
    - Define POST /transfers endpoint with CreateTransferRequest schema
    - Define GET /transfers/{executionId} endpoint with path parameter
    - Define TransferResponse schema with executionId, status, startTime, endTime
    - Define ErrorResponse schema for validation errors
    - Include request/response examples and descriptions
    - Add validation constraints (required fields, formats, enums)
    - _Requirements: 1.1, 1.5, 1.6, 1.7, 1.8, 1.9, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 11.5, 11.6, 11.7_

  - [x] 2.2 Write property test for DTO field completeness
    - **Property 1: DTO Field Completeness**
    - **Validates: Requirements 1.9**
    - Generate random existing DTO instances, verify OpenAPI schema has equivalent properties

- [x] 3. Create OpenAPI specification for Metadata module
  - [x] 3.1 Create src/main/resources/openapi/metadata-api.yaml
    - Define POST /api/metadata/analyze endpoint with targetBaseDir query parameter
    - Define AnalyzeMetadataResponse schema with message and targetBaseDir fields
    - Include request/response examples and descriptions
    - Add validation constraints for required fields
    - _Requirements: 1.2, 1.5, 1.6, 1.7, 1.8, 1.9, 5.1, 5.2, 5.3, 5.4, 5.5, 11.5, 11.6, 11.7_

- [x] 4. Create OpenAPI specification for AI module
  - [x] 4.1 Create src/main/resources/openapi/ai-api.yaml
    - Define POST /ai/analyze endpoint with path query parameter
    - Define AnalyzeAiResponse schema with message and path fields
    - Add pattern validation for JPG/JPEG file extensions
    - Include request/response examples and descriptions
    - _Requirements: 1.3, 1.5, 1.6, 1.7, 1.8, 1.9, 6.1, 6.2, 6.3, 6.4, 6.5, 11.5, 11.6, 11.7_

- [x] 5. Generate and verify DTOs from OpenAPI specifications
  - [x] 5.1 Run Maven generate-sources phase to create DTOs
    - Execute ./mvnw clean generate-sources
    - Verify DTOs created in target/generated-sources/openapi/com/ant/filetrans/{module}/api/model/
    - Verify DTOs are Java records with Jakarta validation annotations
    - Verify DTOs have Jackson annotations for JSON serialization
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.8, 2.9, 8.1, 8.2_

  - [x] 5.2 Write property test for generated code quality
    - **Property 2: Generated Code Quality**
    - **Validates: Requirements 2.4, 2.5, 2.6, 2.9**
    - Generate random OpenAPI schemas, verify generated DTOs are records with proper annotations

  - [x] 5.3 Write unit tests for generated code structure
    - Verify generated DTOs exist in expected packages (transfer.api.model, metadata.api.model, ai.api.model)
    - Verify DTOs are Java records with validation and Jackson annotations
    - _Requirements: 2.4, 2.5, 2.6, 8.8_

- [x] 6. Checkpoint - Verify build and generated code
  - Ensure ./mvnw clean verify passes successfully
  - Verify all generated DTOs compile without errors
  - Ask the user if questions arise

- [x] 7. Implement Transfer module mapper
  - [x] 7.1 Create TransferApiMapper class in com.ant.filetrans.transfer.web.mapper package
    - Implement toCommand(CreateTransferRequest) method to convert DTO to TransferCommand
    - Validate either sourceDir or filePath is provided, throw IllegalArgumentException if both missing
    - Convert string paths to Path objects
    - Parse and normalize extensions list (strip dots, wildcards, lowercase)
    - Wrap extensions in Extensions value object
    - Implement fromJobExecution(JobExecution) method to convert domain to DTO
    - Extract executionId, status, startTime, endTime from JobExecution
    - Convert LocalDateTime to Instant for timestamps
    - Map Spring Batch status enum to string
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6, 8.7_

  - [x] 7.2 Write unit tests for TransferApiMapper
    - Test conversion from valid CreateTransferRequest to TransferCommand
    - Test exception when both sourceDir and filePath are missing
    - Test null handling for optional fields (extensions, sourceDir, filePath)
    - Test extension normalization (strips dots, wildcards, lowercases)
    - Test conversion from JobExecution to TransferResponse
    - Test timestamp conversion from LocalDateTime to Instant
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6_

  - [x] 7.3 Write property test for mapper input conversion
    - **Property 4: Mapper Input Conversion**
    - **Validates: Requirements 3.1, 3.4**
    - Generate random valid CreateTransferRequest DTOs, verify successful conversion to TransferCommand

  - [x] 7.4 Write property test for mapper output conversion
    - **Property 5: Mapper Output Conversion**
    - **Validates: Requirements 3.2**
    - Generate random JobExecution instances, verify successful conversion to TransferResponse

  - [x] 7.5 Write property test for mapper null handling
    - **Property 6: Mapper Null Handling**
    - **Validates: Requirements 3.3**
    - Generate random DTOs with null optional fields, verify mapper handles without exceptions

  - [x] 7.6 Write property test for mapper error reporting
    - **Property 7: Mapper Error Reporting**
    - **Validates: Requirements 3.6**
    - Generate random invalid DTOs, verify mapper throws descriptive exceptions

- [x] 8. Implement Metadata module mapper
  - [x] 8.1 Create MetadataApiMapper class in com.ant.filetrans.metadata.web.mapper package
    - Implement toPath(String targetBaseDir) method to convert string to Path
    - Validate targetBaseDir is not blank, throw IllegalArgumentException if invalid
    - Implement toResponse(String targetBaseDir) method to create AnalyzeMetadataResponse
    - Construct confirmation message with targetBaseDir
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6, 8.7_

  - [x] 8.2 Write unit tests for MetadataApiMapper
    - Test conversion from valid targetBaseDir to Path
    - Test exception when targetBaseDir is blank
    - Test response creation with confirmation message
    - _Requirements: 3.1, 3.2, 3.6_

- [x] 9. Implement AI module mapper
  - [x] 9.1 Create AiApiMapper class in com.ant.filetrans.ai.web.mapper package
    - Implement toPath(String path) method to convert and validate path
    - Validate path is not blank
    - Validate path ends with .jpg or .jpeg (case-insensitive)
    - Throw IllegalArgumentException with descriptive message if validation fails
    - Implement toResponse(String path) method to create AnalyzeAiResponse
    - Construct confirmation message with path
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6, 6.5, 8.7_

  - [x] 9.2 Write unit tests for AiApiMapper
    - Test conversion from valid JPG/JPEG path to Path
    - Test exception when path is blank
    - Test exception when path doesn't end with .jpg or .jpeg
    - Test case-insensitive extension validation
    - Test response creation with confirmation message
    - _Requirements: 3.1, 3.2, 3.6, 6.5_

  - [x] 9.3 Write property test for AI file extension validation
    - **Property 14: AI File Extension Validation**
    - **Validates: Requirements 6.5**
    - Generate random paths with various extensions, verify only JPG/JPEG accepted

- [x] 10. Update FileTransferController to use generated DTOs
  - [x] 10.1 Modify POST /transfers endpoint
    - Change method signature to accept CreateTransferRequest (generated DTO)
    - Add @Valid annotation to trigger Jakarta validation
    - Call TransferApiMapper.toCommand() to convert DTO to domain model
    - Call fileTransferService with domain model
    - Call TransferApiMapper.fromJobExecution() to convert result to DTO
    - Return ResponseEntity.accepted().body(response)
    - _Requirements: 3.1, 3.2, 4.1, 4.2, 4.3, 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x] 10.2 Modify GET /transfers/{executionId} endpoint
    - Keep existing path variable handling
    - Call TransferApiMapper.fromJobExecution() to convert result to DTO
    - Return ResponseEntity.ok().body(response) when found
    - Return ResponseEntity.notFound().build() when not found
    - _Requirements: 3.2, 4.4, 4.5, 4.6, 9.1, 9.4, 9.5_

  - [x] 10.3 Write unit tests for FileTransferController with MockMvc
    - Test POST /transfers returns 202 with valid request
    - Test POST /transfers returns 400 when targetBaseDir missing
    - Test POST /transfers returns 400 when both sourceDir and filePath missing
    - Test GET /transfers/{executionId} returns 200 when execution found
    - Test GET /transfers/{executionId} returns 404 when execution not found
    - Mock fileTransferService to isolate controller logic
    - _Requirements: 4.2, 4.3, 4.5, 4.6_

  - [x] 10.4 Write property test for valid request success response
    - **Property 8: Valid Request Success Response**
    - **Validates: Requirements 4.2**
    - Generate random valid transfer requests, verify endpoint returns 202 with executionId

  - [x] 10.5 Write property test for invalid request rejection
    - **Property 9: Invalid Request Rejection**
    - **Validates: Requirements 4.3**
    - Generate random invalid requests, verify endpoint returns 400

  - [x] 10.6 Write property test for execution ID lookup success
    - **Property 10: Execution ID Lookup Success**
    - **Validates: Requirements 4.5**
    - Generate random valid executionIds, verify endpoint returns 200 with TransferResponse

  - [x] 10.7 Write property test for execution ID lookup failure
    - **Property 11: Execution ID Lookup Failure**
    - **Validates: Requirements 4.6**
    - Generate random non-existent executionIds, verify endpoint returns 404

- [x] 11. Update MetadataController to use generated DTOs
  - [x] 11.1 Modify POST /api/metadata/analyze endpoint
    - Change method signature to accept targetBaseDir as @RequestParam
    - Add validation for required parameter
    - Call MetadataApiMapper.toPath() to convert string to Path
    - Call metadata service with Path
    - Call MetadataApiMapper.toResponse() to create response DTO
    - Return ResponseEntity.accepted().body(response)
    - _Requirements: 3.1, 3.2, 5.1, 5.2, 5.3, 5.4, 5.5, 9.1, 9.4, 9.5_

  - [x] 11.2 Write unit tests for MetadataController with MockMvc
    - Test POST /api/metadata/analyze returns 202 with valid targetBaseDir
    - Test POST /api/metadata/analyze returns 400 when targetBaseDir missing
    - Mock metadata service to isolate controller logic
    - _Requirements: 5.2, 5.3_

  - [x] 11.3 Write property test for metadata analysis success
    - **Property 12: Metadata Analysis Success**
    - **Validates: Requirements 5.2**
    - Generate random valid targetBaseDir values, verify endpoint returns 202

- [x] 12. Update AiAnalysisController to use generated DTOs
  - [x] 12.1 Modify POST /ai/analyze endpoint
    - Change method signature to accept path as @RequestParam
    - Add validation for required parameter
    - Call AiApiMapper.toPath() to convert and validate path
    - Call AI service with Path
    - Call AiApiMapper.toResponse() to create response DTO
    - Return ResponseEntity.ok().body(response) or ResponseEntity.accepted().body(response)
    - _Requirements: 3.1, 3.2, 6.1, 6.2, 6.3, 6.4, 6.5, 9.1, 9.4, 9.5_

  - [x] 12.2 Write unit tests for AiAnalysisController with MockMvc
    - Test POST /ai/analyze returns 200 or 202 with valid JPG path
    - Test POST /ai/analyze returns 400 when path missing
    - Test POST /ai/analyze returns 400 when path is not JPG/JPEG
    - Mock AI service to isolate controller logic
    - _Requirements: 6.2, 6.3, 6.5_

  - [x] 12.3 Write property test for AI analysis success
    - **Property 13: AI Analysis Success**
    - **Validates: Requirements 6.2**
    - Generate random valid JPG/JPEG paths, verify endpoint returns 200 or 202

- [x] 13. Checkpoint - Verify controllers and mappers
  - Ensure all unit tests pass
  - Ensure all property tests pass
  - Verify backward compatibility with existing API contracts
  - Ask the user if questions arise

- [x] 14. Implement validation error handling
  - [x] 14.1 Create or update global exception handler
    - Add @ControllerAdvice class if not exists
    - Handle MethodArgumentNotValidException for Jakarta validation errors
    - Handle IllegalArgumentException from mappers
    - Return 400 Bad Request with descriptive error messages
    - Include field name, validation constraint, and expected format in error response
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

  - [x] 14.2 Write unit tests for validation error handling
    - Test missing required field returns 400 with field name in message
    - Test invalid format returns 400 with expected format in message
    - Test pattern mismatch returns 400 with pattern description
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 14.3 Write property test for validation error reporting
    - **Property 19: Validation Error Reporting**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
    - Generate random constraint violations, verify error messages identify field and describe violation

- [x] 15. Write integration tests for complete API flows
  - [x] 15.1 Write integration test for Transfer API
    - Start Spring Boot test context
    - Test POST /transfers creates job and returns 202
    - Test GET /transfers/{executionId} retrieves job status
    - Test end-to-end flow with real HTTP requests
    - Verify JSON structure matches OpenAPI schemas
    - _Requirements: 4.1, 4.2, 4.4, 4.5, 9.1, 9.7_

  - [x] 15.2 Write integration test for Metadata API
    - Start Spring Boot test context
    - Test POST /api/metadata/analyze triggers analysis and returns 202
    - Verify JSON structure matches OpenAPI schema
    - _Requirements: 5.1, 5.2, 9.1, 9.7_

  - [x] 15.3 Write integration test for AI API
    - Start Spring Boot test context
    - Test POST /ai/analyze triggers analysis and returns 200 or 202
    - Verify JSON structure matches OpenAPI schema
    - _Requirements: 6.1, 6.2, 9.1, 9.7_

  - [x] 15.4 Write integration test for Swagger UI
    - Test /v3/api-docs endpoint serves OpenAPI JSON
    - Test /swagger-ui.html endpoint serves Swagger UI page
    - Verify all endpoints documented in OpenAPI JSON
    - _Requirements: 11.1, 11.2, 11.3_

  - [x] 15.5 Write property test for API contract preservation
    - **Property 18: API Contract Preservation**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.7**
    - Generate random data, verify JSON serialization identical between old and new DTOs

- [x] 16. Write build integration tests
  - [x] 16.1 Write test for OpenAPI code generation
    - Test Maven build generates DTOs from specifications
    - Test generated sources directory contains expected files
    - Test generated code compiles successfully
    - _Requirements: 2.1, 2.2, 2.3, 7.1, 7.2, 7.6_

  - [x] 16.2 Write test for incremental build optimization
    - **Property 15: Incremental Build Optimization**
    - **Validates: Requirements 7.4**
    - Test unchanged OpenAPI specs don't trigger regeneration

  - [x] 16.3 Write test for invalid specification build failure
    - **Property 16: Invalid Specification Build Failure**
    - **Validates: Requirements 7.5**
    - Test OpenAPI specs with syntax errors cause build failure

  - [x] 16.4 Write property test for specification change propagation
    - **Property 3: Specification Change Propagation**
    - **Validates: Requirements 2.7**
    - Modify OpenAPI spec, verify Maven build regenerates affected DTOs

- [x] 17. Write modulith boundary tests
  - [x] 17.1 Write test for module package structure
    - Verify Transfer DTOs in com.ant.filetrans.transfer.api.model
    - Verify Metadata DTOs in com.ant.filetrans.metadata.api.model
    - Verify AI DTOs in com.ant.filetrans.ai.api.model
    - _Requirements: 8.1, 8.2, 8.3_

  - [x] 17.2 Write test for mapper module isolation
    - Verify mappers don't import classes from other modules (except events)
    - Verify no cross-module schema dependencies
    - _Requirements: 8.4, 8.5, 8.6_

  - [x] 17.3 Run ModulithStructureTests
    - Verify modulith boundaries respected with generated DTOs
    - Verify no circular dependencies introduced
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 18. Final checkpoint and cleanup
  - Ensure ./mvnw clean verify passes with all tests
  - Verify Swagger UI accessible at http://localhost:8080/swagger-ui.html
  - Verify all endpoints documented and testable in Swagger UI
  - Remove any old hand-written DTOs if they exist
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (minimum 100 iterations each)
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end behavior with real Spring context
- All generated code is in target/generated-sources/openapi and should not be committed to version control
- OpenAPI YAML files in src/main/resources/openapi are the source of truth for API contracts
