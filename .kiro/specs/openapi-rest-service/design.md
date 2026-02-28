# OpenAPI REST Service - Design Document

## Overview

This design introduces formal OpenAPI specifications and automated code generation for the filetrans application's REST APIs. The system currently has three Spring Boot Modulith modules (transfer, metadata, ai) with hand-written REST controllers and DTOs. This feature will:

1. Create OpenAPI 3.0 specifications for all existing REST endpoints
2. Configure Maven-based code generation to produce Java DTOs from OpenAPI schemas
3. Implement mappers to convert between generated DTOs and internal domain models
4. Maintain strict modulith boundaries through package structure and code organization
5. Enable Swagger UI for interactive API documentation

The design preserves backward compatibility - existing API contracts remain unchanged while gaining formal documentation and type-safe code generation.

## Architecture

### High-Level Structure

```
filetrans/
├── src/main/resources/openapi/
│   ├── transfer-api.yaml          # Transfer module OpenAPI spec
│   ├── metadata-api.yaml          # Metadata module OpenAPI spec
│   └── ai-api.yaml                # AI module OpenAPI spec
├── target/generated-sources/openapi/
│   └── com/ant/filetrans/
│       ├── transfer/api/model/    # Generated transfer DTOs
│       ├── metadata/api/model/    # Generated metadata DTOs
│       └── ai/api/model/          # Generated AI DTOs
└── src/main/java/com/ant/filetrans/
    ├── transfer/web/
    │   ├── FileTransferController.java  # Updated to use generated DTOs
    │   └── mapper/
    │       └── TransferApiMapper.java   # Maps generated DTO ↔ domain
    ├── metadata/web/
    │   ├── MetadataController.java
    │   └── mapper/
    │       └── MetadataApiMapper.java
    └── ai/web/
        ├── AiAnalysisController.java
        └── mapper/
            └── AiApiMapper.java
```

### Code Generation Flow

```
[OpenAPI YAML] 
    ↓ (Maven generate-sources phase)
[openapi-generator-maven-plugin]
    ↓
[Generated Java Records in target/generated-sources]
    ↓ (Maven compile phase)
[Compiled classes available to controllers]
```

### Request/Response Flow

```
HTTP Request
    ↓
[Spring MVC + Validation]
    ↓ (validates Jakarta annotations)
[Controller receives Generated DTO]
    ↓
[Mapper converts to Internal Domain Model]
    ↓
[Service layer processes domain model]
    ↓
[Mapper converts domain model to Generated DTO]
    ↓
[Controller returns Generated DTO]
    ↓
HTTP Response (JSON)
```

## Components and Interfaces

### 1. OpenAPI Specifications

Each module has its own OpenAPI specification file located in `src/main/resources/openapi/`.

#### Transfer API Specification (`transfer-api.yaml`)

Defines:
- `POST /transfers` - Create transfer job (directory or single file)
- `GET /transfers/{executionId}` - Query transfer job status

Schemas:
- `CreateTransferRequest` - Input for creating transfers
  - `sourceDir` (string, optional) - Source directory for bulk transfer
  - `filePath` (string, optional) - Single file path for individual transfer
  - `targetBaseDir` (string, required) - Destination base directory
  - `extensions` (array of strings, optional) - File extension filter
  - Validation: At least one of sourceDir or filePath must be provided
- `TransferResponse` - Transfer job status
  - `executionId` (integer, required) - Job execution ID
  - `status` (string, required) - Job status (STARTING, STARTED, COMPLETED, FAILED, etc.)
  - `startTime` (string, date-time, optional) - Job start timestamp
  - `endTime` (string, date-time, optional) - Job end timestamp

#### Metadata API Specification (`metadata-api.yaml`)

Defines:
- `POST /api/metadata/analyze` - Trigger metadata extraction for directory

Schemas:
- `AnalyzeMetadataRequest` - Input for metadata analysis
  - `targetBaseDir` (string, required) - Directory to analyze
- `AnalyzeMetadataResponse` - Analysis trigger confirmation
  - `message` (string, required) - Confirmation message
  - `targetBaseDir` (string, required) - Directory being analyzed

#### AI API Specification (`ai-api.yaml`)

Defines:
- `POST /ai/analyze` - Trigger AI analysis for specific image

Schemas:
- `AnalyzeAiRequest` - Input for AI analysis
  - `path` (string, required) - Image file path
  - Validation: Must end with .jpg or .jpeg (case-insensitive)
- `AnalyzeAiResponse` - Analysis trigger confirmation
  - `message` (string, required) - Confirmation message
  - `path` (string, required) - Image path being analyzed

### 2. Maven Plugin Configuration

The `openapi-generator-maven-plugin` is configured in `pom.xml` with three executions (one per module).

Configuration per execution:
- **Phase**: `generate-sources` (runs before compile)
- **Goal**: `generate`
- **Generator**: `java` (generates Java code)
- **Library**: `native` (uses Java HTTP client, no external dependencies)
- **Input**: `src/main/resources/openapi/{module}-api.yaml`
- **Output**: `target/generated-sources/openapi`
- **Model Package**: `com.ant.filetrans.{module}.api.model`
- **Additional Properties**:
  - `useJakartaEe=true` - Use Jakarta validation annotations
  - `dateLibrary=java8` - Use Java 8+ date/time types
  - `generateApis=false` - Only generate models, not API interfaces
  - `generateApiTests=false` - Skip API test generation
  - `generateModelTests=false` - Skip model test generation
  - `generateModelDocumentation=false` - Skip model documentation
  - `generateSupportingFiles=false` - Skip supporting files
  - `useBeanValidation=true` - Include Jakarta Bean Validation annotations
  - `performBeanValidation=true` - Enable validation
  - `useOptional=false` - Use nullable types instead of Optional
  - `additionalModelTypeAnnotations=@lombok.Builder` - Add Lombok builder

### 3. Generated DTOs

Generated DTOs are Java records with:
- Immutable fields
- Jackson annotations for JSON serialization (`@JsonProperty`, `@JsonFormat`)
- Jakarta validation annotations (`@NotNull`, `@Pattern`, `@Size`)
- Lombok `@Builder` annotation for convenient construction
- Generated in `target/generated-sources/openapi/com/ant/filetrans/{module}/api/model/`

Example generated DTO structure:
```java
@lombok.Builder
public record CreateTransferRequest(
    @JsonProperty("sourceDir")
    String sourceDir,
    
    @JsonProperty("targetBaseDir")
    @NotNull
    String targetBaseDir,
    
    @JsonProperty("filePath")
    String filePath,
    
    @JsonProperty("extensions")
    List<String> extensions
) {}
```

### 4. API Mappers

Each module has a mapper class in its `web.mapper` package that converts between generated DTOs and internal domain models.

#### TransferApiMapper

Location: `com.ant.filetrans.transfer.web.mapper.TransferApiMapper`

Methods:
- `TransferCommand toCommand(CreateTransferRequest dto)` - Converts generated DTO to internal command
  - Validates that either sourceDir or filePath is provided
  - Converts string paths to `Path` objects
  - Parses and normalizes extensions list
  - Wraps extensions in `Extensions` value object
- `TransferResponse fromJobExecution(JobExecution execution)` - Converts Spring Batch execution to generated DTO
  - Extracts execution ID, status, timestamps
  - Converts `LocalDateTime` to `Instant`
  - Maps Spring Batch status enum to string

#### MetadataApiMapper

Location: `com.ant.filetrans.metadata.web.mapper.MetadataApiMapper`

Methods:
- `Path toPath(AnalyzeMetadataRequest dto)` - Extracts and validates path from request
  - Validates targetBaseDir is not blank
  - Converts string to `Path` object
- `AnalyzeMetadataResponse toResponse(String targetBaseDir)` - Creates response DTO
  - Constructs confirmation message
  - Returns generated DTO

#### AiApiMapper

Location: `com.ant.filetrans.ai.web.mapper.AiApiMapper`

Methods:
- `Path toPath(AnalyzeAiRequest dto)` - Extracts and validates path from request
  - Validates path is not blank
  - Validates path ends with .jpg or .jpeg (case-insensitive)
  - Converts string to `Path` object
  - Throws `IllegalArgumentException` if validation fails
- `AnalyzeAiResponse toResponse(String path)` - Creates response DTO
  - Constructs confirmation message
  - Returns generated DTO

### 5. Controller Updates

Controllers are updated to:
- Accept generated DTOs as `@RequestBody` or `@RequestParam`
- Use `@Valid` annotation to trigger Jakarta validation
- Call mapper to convert DTO to domain model
- Call service layer with domain model
- Call mapper to convert result to generated DTO
- Return generated DTO in `ResponseEntity`

Example controller method signature:
```java
@PostMapping
public ResponseEntity<TransferResponse> createTransfer(
    @Valid @RequestBody CreateTransferRequest request
) throws Exception {
    TransferCommand command = TransferApiMapper.toCommand(request);
    JobExecution execution = fileTransferService.transferDirectory(...);
    TransferResponse response = TransferApiMapper.fromJobExecution(execution);
    return ResponseEntity.accepted().body(response);
}
```

### 6. Swagger UI Integration

Spring Boot auto-configuration provides:
- `/v3/api-docs` - OpenAPI specification in JSON format (aggregated from all modules)
- `/swagger-ui.html` - Interactive Swagger UI for testing endpoints

Configuration in `application.yaml`:
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

Dependencies required:
- `springdoc-openapi-starter-webmvc-ui` - Provides Swagger UI and OpenAPI endpoint generation

## Data Models

### Transfer Module

**CreateTransferRequest** (Generated DTO)
- Purpose: Accept file transfer job creation requests
- Fields:
  - `sourceDir`: Optional source directory for bulk transfer
  - `targetBaseDir`: Required destination base directory
  - `filePath`: Optional single file path for individual transfer
  - `extensions`: Optional list of file extensions to filter
- Validation:
  - `targetBaseDir` is required (not null)
  - At least one of `sourceDir` or `filePath` must be provided (enforced by mapper)

**TransferResponse** (Generated DTO)
- Purpose: Return transfer job status information
- Fields:
  - `executionId`: Job execution ID (Long)
  - `status`: Job status string (STARTING, STARTED, COMPLETED, FAILED, etc.)
  - `startTime`: Job start timestamp (Instant, nullable)
  - `endTime`: Job end timestamp (Instant, nullable)

**TransferCommand** (Internal Domain Model)
- Purpose: Internal representation of transfer operation
- Fields:
  - `sourceDir`: Path object (nullable)
  - `targetBaseDir`: Path object (required)
  - `filePath`: Path object (nullable)
  - `extensions`: Extensions value object
- Not exposed outside transfer module

### Metadata Module

**AnalyzeMetadataRequest** (Generated DTO)
- Purpose: Accept metadata analysis requests
- Fields:
  - `targetBaseDir`: Required directory path to analyze
- Validation:
  - `targetBaseDir` is required (not null)

**AnalyzeMetadataResponse** (Generated DTO)
- Purpose: Confirm metadata analysis was triggered
- Fields:
  - `message`: Confirmation message
  - `targetBaseDir`: Directory being analyzed

### AI Module

**AnalyzeAiRequest** (Generated DTO)
- Purpose: Accept AI analysis requests for images
- Fields:
  - `path`: Required image file path
- Validation:
  - `path` is required (not null)
  - `path` must match pattern `.*\.(jpg|jpeg)$` (case-insensitive)

**AnalyzeAiResponse** (Generated DTO)
- Purpose: Confirm AI analysis was triggered
- Fields:
  - `message`: Confirmation message
  - `path`: Image path being analyzed

### Module Boundary Preservation

- Generated DTOs are in `{module}.api.model` packages
- Internal domain models remain in `{module}.application` or `{module}.domain` packages
- Mappers are in `{module}.web.mapper` packages
- No cross-module DTO dependencies - each module has independent schemas
- Event types (FileTransferredEvent, AiAnalysisCompletedEvent) remain in their respective modules and are not part of REST API contracts



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified the following testable properties and performed redundancy analysis:

**Redundancy Findings:**
- Properties 9.1 and 9.7 both test JSON structure preservation - these can be combined into a single comprehensive property
- Properties 9.2 and 9.3 (field name and type matching) are subsumed by the JSON structure property
- Properties 10.2, 10.3, 10.4, and 10.5 all test error message content - these can be combined into a single property about descriptive error messages
- Properties 2.4, 2.5, and 2.6 all test generated DTO characteristics - these can be combined into a single property about generated code quality

**Consolidated Properties:**
The following properties represent unique validation value after removing redundancy:

### Property 1: DTO Field Completeness

*For any* existing hand-written DTO field, the OpenAPI schema SHALL include an equivalent property with the same name and compatible type.

**Validates: Requirements 1.9**

### Property 2: Generated Code Quality

*For any* OpenAPI schema, the generated DTO SHALL be a Java record with Jakarta validation annotations corresponding to OpenAPI constraints, Jackson annotations for JSON serialization, and support for Java 25 language features.

**Validates: Requirements 2.4, 2.5, 2.6, 2.9**

### Property 3: Specification Change Propagation

*For any* modification to an OpenAPI specification, running the Maven build SHALL regenerate the affected DTO classes to reflect the changes.

**Validates: Requirements 2.7**

### Property 4: Mapper Input Conversion

*For any* generated DTO representing API input, the mapper SHALL successfully convert it to the corresponding internal domain model with appropriate data transformations (string to Path, list to Set, etc.).

**Validates: Requirements 3.1, 3.4**

### Property 5: Mapper Output Conversion

*For any* internal domain model that must be returned via API, the mapper SHALL successfully convert it to the corresponding generated DTO.

**Validates: Requirements 3.2**

### Property 6: Mapper Null Handling

*For any* optional field in a generated DTO, the mapper SHALL correctly handle null values according to the API contract without throwing exceptions.

**Validates: Requirements 3.3**

### Property 7: Mapper Error Reporting

*For any* invalid data that causes mapping to fail, the mapper SHALL throw an exception with a descriptive message identifying the problem.

**Validates: Requirements 3.6**

### Property 8: Valid Request Success Response

*For any* valid transfer request, the POST /transfers endpoint SHALL return 202 Accepted status with a TransferResponse body containing a non-null executionId.

**Validates: Requirements 4.2**

### Property 9: Invalid Request Rejection

*For any* transfer request missing required fields (targetBaseDir and both sourceDir/filePath), the POST /transfers endpoint SHALL return 400 Bad Request.

**Validates: Requirements 4.3**

### Property 10: Execution ID Lookup Success

*For any* valid executionId that exists in the system, the GET /transfers/{executionId} endpoint SHALL return 200 OK with a TransferResponse body.

**Validates: Requirements 4.5**

### Property 11: Execution ID Lookup Failure

*For any* executionId that does not exist in the system, the GET /transfers/{executionId} endpoint SHALL return 404 Not Found.

**Validates: Requirements 4.6**

### Property 12: Metadata Analysis Success

*For any* valid targetBaseDir parameter, the POST /api/metadata/analyze endpoint SHALL return 202 Accepted with a response body.

**Validates: Requirements 5.2**

### Property 13: AI Analysis Success

*For any* valid path parameter pointing to a JPG or JPEG file, the POST /ai/analyze endpoint SHALL return 200 OK or 202 Accepted.

**Validates: Requirements 6.2**

### Property 14: AI File Extension Validation

*For any* path parameter that does not end with .jpg or .jpeg (case-insensitive), the POST /ai/analyze endpoint SHALL reject the request.

**Validates: Requirements 6.5**

### Property 15: Incremental Build Optimization

*For any* unchanged OpenAPI specification, running the Maven build SHALL not regenerate the corresponding DTO classes.

**Validates: Requirements 7.4**

### Property 16: Invalid Specification Build Failure

*For any* OpenAPI specification containing YAML syntax errors, the Maven build SHALL fail with an error message.

**Validates: Requirements 7.5**

### Property 17: Generated Code Compilation

*For any* generated DTO code, the Maven compile phase SHALL successfully compile the code without errors.

**Validates: Requirements 7.6**

### Property 18: API Contract Preservation

*For any* existing API endpoint, replacing hand-written DTOs with generated DTOs SHALL produce identical JSON request/response structures for the same data.

**Validates: Requirements 9.1, 9.2, 9.3, 9.7**

### Property 19: Validation Error Reporting

*For any* request that violates OpenAPI constraints (missing required field, invalid format, etc.), the system SHALL return 400 Bad Request with an error message that identifies the field, describes the violation, and explains the expected format.

**Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**

### Property 20: OpenAPI Documentation Completeness

*For any* endpoint defined in the OpenAPI specifications, the specification SHALL include a description for the endpoint and descriptions for all schema properties.

**Validates: Requirements 11.5, 11.6**

### Property 21: Request Schema Examples

*For any* request schema in the OpenAPI specifications, the schema SHALL include example values demonstrating valid requests.

**Validates: Requirements 11.7**

## Error Handling

### Validation Errors

**Trigger**: Request violates OpenAPI constraints (missing required field, invalid format, pattern mismatch)

**Handling**:
- Spring MVC validation interceptor catches `MethodArgumentNotValidException`
- Returns 400 Bad Request
- Response body includes:
  - Field name that failed validation
  - Validation constraint that was violated
  - Expected format or pattern
- Example response:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for argument [0]: Field 'targetBaseDir' must not be null",
  "path": "/transfers"
}
```

### Mapper Conversion Errors

**Trigger**: Mapper cannot convert DTO to domain model (e.g., invalid path format, business rule violation)

**Handling**:
- Mapper throws `IllegalArgumentException` with descriptive message
- Controller catches exception
- Returns 400 Bad Request with error details
- Example: "Invalid transfer request: either sourceDir or filePath must be provided"

### Resource Not Found

**Trigger**: GET request for non-existent resource (e.g., executionId doesn't exist)

**Handling**:
- Service layer returns null
- Controller checks for null
- Returns 404 Not Found
- No response body (or minimal error message)

### File System Errors

**Trigger**: Invalid path, permission denied, file doesn't exist

**Handling**:
- Service layer catches `IOException` or `InvalidPathException`
- Logs error with context
- Returns 500 Internal Server Error
- Response includes generic error message (no sensitive path information)

### Build-Time Errors

**Trigger**: OpenAPI specification has syntax errors or generates invalid code

**Handling**:
- Maven plugin fails during generate-sources phase
- Build stops before compilation
- Error message indicates which specification file has errors
- Developer must fix specification and rebuild

## Testing Strategy

### Unit Testing

Unit tests focus on specific examples, edge cases, and component isolation.

**OpenAPI Specification Validation Tests**
- Parse each YAML file and verify it's valid OpenAPI 3.0+
- Check that all required endpoints are defined (POST /transfers, GET /transfers/{executionId}, etc.)
- Verify schemas include required fields and validation constraints
- Confirm response definitions include success and error status codes
- Example test: `TransferApiSpecificationTest.shouldDefineAllRequiredEndpoints()`

**Mapper Unit Tests**
- Test conversion from generated DTO to domain model with valid data
- Test conversion from domain model to generated DTO
- Test null handling for optional fields
- Test exception throwing for invalid data
- Test specific transformations (string to Path, list to Set, extension normalization)
- Example tests:
  - `TransferApiMapperTest.shouldConvertValidRequestToCommand()`
  - `TransferApiMapperTest.shouldThrowExceptionWhenBothSourceDirAndFilePathAreMissing()`
  - `TransferApiMapperTest.shouldHandleNullExtensions()`
  - `TransferApiMapperTest.shouldNormalizeExtensions()` (strips dots, wildcards, lowercases)

**Controller Unit Tests** (with MockMvc)
- Test endpoint paths and HTTP methods
- Test request validation (missing required fields, invalid formats)
- Test successful responses (status codes, response body structure)
- Test error responses (404 for missing resources, 400 for validation errors)
- Mock service layer to isolate controller logic
- Example tests:
  - `FileTransferControllerTest.shouldReturn202WhenValidRequestProvided()`
  - `FileTransferControllerTest.shouldReturn400WhenTargetBaseDirMissing()`
  - `FileTransferControllerTest.shouldReturn404WhenExecutionIdNotFound()`

**Generated Code Verification Tests**
- After build, verify generated DTOs exist in expected packages
- Verify generated DTOs are Java records
- Verify generated DTOs have Jakarta validation annotations
- Verify generated DTOs have Jackson annotations
- Example test: `GeneratedCodeStructureTest.shouldGenerateRecordsWithValidationAnnotations()`

### Property-Based Testing

Property tests verify universal properties across many generated inputs. Each test runs minimum 100 iterations.

**Property Test 1: DTO Field Completeness**
- Generate: Random existing DTO instances
- Property: For each field in existing DTO, OpenAPI schema has equivalent property
- Verification: Parse OpenAPI YAML, check schema properties match DTO fields
- Tag: **Feature: openapi-rest-service, Property 1: For any existing hand-written DTO field, the OpenAPI schema SHALL include an equivalent property**

**Property Test 2: Generated Code Quality**
- Generate: Random OpenAPI schemas with various constraints
- Property: Generated DTOs are records with proper annotations
- Verification: Compile generated code, reflect on classes, verify annotations present
- Tag: **Feature: openapi-rest-service, Property 2: For any OpenAPI schema, the generated DTO SHALL be a Java record with Jakarta validation annotations**

**Property Test 3: Mapper Input Conversion**
- Generate: Random valid generated DTOs (various combinations of sourceDir/filePath, extensions)
- Property: Mapper successfully converts to domain model without exceptions
- Verification: Call mapper, assert domain model fields match DTO fields
- Tag: **Feature: openapi-rest-service, Property 4: For any generated DTO representing API input, the mapper SHALL successfully convert it to internal domain model**

**Property Test 4: Mapper Output Conversion**
- Generate: Random domain model instances (JobExecution with various statuses, timestamps)
- Property: Mapper successfully converts to generated DTO
- Verification: Call mapper, assert DTO fields populated correctly
- Tag: **Feature: openapi-rest-service, Property 5: For any internal domain model, the mapper SHALL successfully convert it to generated DTO**

**Property Test 5: Mapper Null Handling**
- Generate: Random DTOs with null optional fields
- Property: Mapper handles nulls without throwing exceptions
- Verification: Call mapper with null fields, assert no exceptions, verify domain model handles nulls
- Tag: **Feature: openapi-rest-service, Property 6: For any optional field, the mapper SHALL correctly handle null values**

**Property Test 6: Mapper Error Reporting**
- Generate: Random invalid DTOs (missing required fields, invalid formats)
- Property: Mapper throws descriptive exception
- Verification: Call mapper, catch exception, assert message contains field name and problem description
- Tag: **Feature: openapi-rest-service, Property 7: For any invalid data, the mapper SHALL throw an exception with a descriptive message**

**Property Test 7: Valid Request Success Response**
- Generate: Random valid transfer requests
- Property: Endpoint returns 202 with non-null executionId
- Verification: POST to /transfers, assert status 202, assert response body has executionId
- Tag: **Feature: openapi-rest-service, Property 8: For any valid transfer request, the endpoint SHALL return 202 Accepted**

**Property Test 8: Invalid Request Rejection**
- Generate: Random invalid transfer requests (missing targetBaseDir, missing both sourceDir and filePath)
- Property: Endpoint returns 400
- Verification: POST to /transfers, assert status 400
- Tag: **Feature: openapi-rest-service, Property 9: For any invalid transfer request, the endpoint SHALL return 400 Bad Request**

**Property Test 9: API Contract Preservation**
- Generate: Random data instances
- Property: JSON serialization of old DTO equals JSON serialization of new generated DTO
- Verification: Serialize both DTOs to JSON, compare strings
- Tag: **Feature: openapi-rest-service, Property 18: For any data, replacing hand-written DTOs with generated DTOs SHALL produce identical JSON**

**Property Test 10: Validation Error Reporting**
- Generate: Random constraint violations (missing required fields, invalid patterns)
- Property: Error response identifies field and describes violation
- Verification: POST invalid request, parse error response, assert field name and constraint present
- Tag: **Feature: openapi-rest-service, Property 19: For any constraint violation, the error message SHALL identify the field and describe the violation**

### Integration Testing

Integration tests verify end-to-end behavior with real Spring context.

**Build Integration Tests**
- Run Maven build with test OpenAPI specifications
- Verify generated sources directory contains expected files
- Verify generated code compiles successfully
- Verify incremental builds don't regenerate unchanged specs
- Example test: `OpenApiCodeGenerationIntegrationTest.shouldGenerateSourcesFromSpecifications()`

**API Integration Tests**
- Start Spring Boot application with test context
- Make real HTTP requests to endpoints
- Verify request/response JSON structure matches OpenAPI schemas
- Verify validation errors return proper status codes and messages
- Verify Swagger UI endpoint serves documentation
- Example tests:
  - `TransferApiIntegrationTest.shouldCreateAndQueryTransferJob()`
  - `MetadataApiIntegrationTest.shouldTriggerMetadataAnalysis()`
  - `AiApiIntegrationTest.shouldTriggerAiAnalysis()`
  - `SwaggerUiIntegrationTest.shouldServeOpenApiDocumentation()`

**Modulith Boundary Tests**
- Verify generated DTOs are in correct packages (transfer.api.model, metadata.api.model, ai.api.model)
- Verify mappers don't import classes from other modules (except events)
- Verify no cross-module schema dependencies
- Example test: `ModulithStructureTests.shouldRespectModuleBoundaries()`

### Test Configuration

**Property-Based Testing Library**: Use `jqwik` for Java property-based testing
- Minimum 100 iterations per property test
- Configure in pom.xml:
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

**Test Execution**:
- Unit tests: `./mvnw test`
- Integration tests: `./mvnw verify`
- Property tests run as part of unit test suite
- All tests must pass before merge

**Test Data Generators** (for property tests):
- `CreateTransferRequestArbitrary` - Generates random valid/invalid transfer requests
- `JobExecutionArbitrary` - Generates random Spring Batch JobExecution instances
- `PathArbitrary` - Generates random valid/invalid file paths
- `ExtensionsArbitrary` - Generates random extension lists



## Implementation Details

### Maven Plugin Configuration

Add to `pom.xml` in the `<build><plugins>` section:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.2.0</version>
    <executions>
        <!-- Transfer Module -->
        <execution>
            <id>generate-transfer-api</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi/transfer-api.yaml</inputSpec>
                <generatorName>java</generatorName>
                <library>native</library>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <modelPackage>com.ant.filetrans.transfer.api.model</modelPackage>
                <generateApis>false</generateApis>
                <generateModels>true</generateModels>
                <generateModelTests>false</generateModelTests>
                <generateModelDocumentation>false</generateModelDocumentation>
                <generateSupportingFiles>false</generateSupportingFiles>
                <configOptions>
                    <useJakartaEe>true</useJakartaEe>
                    <dateLibrary>java8</dateLibrary>
                    <useBeanValidation>true</useBeanValidation>
                    <performBeanValidation>true</performBeanValidation>
                    <useOptional>false</useOptional>
                    <additionalModelTypeAnnotations>@lombok.Builder</additionalModelTypeAnnotations>
                </configOptions>
            </configuration>
        </execution>
        
        <!-- Metadata Module -->
        <execution>
            <id>generate-metadata-api</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi/metadata-api.yaml</inputSpec>
                <generatorName>java</generatorName>
                <library>native</library>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <modelPackage>com.ant.filetrans.metadata.api.model</modelPackage>
                <generateApis>false</generateApis>
                <generateModels>true</generateModels>
                <generateModelTests>false</generateModelTests>
                <generateModelDocumentation>false</generateModelDocumentation>
                <generateSupportingFiles>false</generateSupportingFiles>
                <configOptions>
                    <useJakartaEe>true</useJakartaEe>
                    <dateLibrary>java8</dateLibrary>
                    <useBeanValidation>true</useBeanValidation>
                    <performBeanValidation>true</performBeanValidation>
                    <useOptional>false</useOptional>
                    <additionalModelTypeAnnotations>@lombok.Builder</additionalModelTypeAnnotations>
                </configOptions>
            </configuration>
        </execution>
        
        <!-- AI Module -->
        <execution>
            <id>generate-ai-api</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi/ai-api.yaml</inputSpec>
                <generatorName>java</generatorName>
                <library>native</library>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <modelPackage>com.ant.filetrans.ai.api.model</modelPackage>
                <generateApis>false</generateApis>
                <generateModels>true</generateModels>
                <generateModelTests>false</generateModelTests>
                <generateModelDocumentation>false</generateModelDocumentation>
                <generateSupportingFiles>false</generateSupportingFiles>
                <configOptions>
                    <useJakartaEe>true</useJakartaEe>
                    <dateLibrary>java8</dateLibrary>
                    <useBeanValidation>true</useBeanValidation>
                    <performBeanValidation>true</performBeanValidation>
                    <useOptional>false</useOptional>
                    <additionalModelTypeAnnotations>@lombok.Builder</additionalModelTypeAnnotations>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Add generated sources to build path -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>add-generated-sources</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>add-source</goal>
            </goals>
            <configuration>
                <sources>
                    <source>${project.build.directory}/generated-sources/openapi</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Dependencies

Add to `pom.xml` in the `<dependencies>` section:

```xml
<!-- OpenAPI/Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- Jakarta Validation API (already included via spring-boot-starter-validation) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Property-based testing -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

### OpenAPI Specification Templates

#### Transfer API (`src/main/resources/openapi/transfer-api.yaml`)

```yaml
openapi: 3.0.3
info:
  title: File Transfer API
  description: API for managing file transfer jobs
  version: 1.0.0
servers:
  - url: http://localhost:8080
    description: Local development server

paths:
  /transfers:
    post:
      summary: Create a new file transfer job
      description: |
        Creates a new transfer job for either a directory (bulk transfer) or a single file.
        Provide either sourceDir for directory transfer or filePath for single file transfer.
      operationId: createTransfer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTransferRequest'
            examples:
              directoryTransfer:
                summary: Directory transfer
                value:
                  sourceDir: "/Volumes/Camera/DCIM/101NZ7_2"
                  targetBaseDir: "/Users/anton/Pictures/Imported"
                  extensions: ["jpg", "jpeg", "raw"]
              singleFileTransfer:
                summary: Single file transfer
                value:
                  filePath: "/Volumes/Camera/DCIM/101NZ7_2/DSC_2345.JPG"
                  targetBaseDir: "/Users/anton/Pictures/Imported"
      responses:
        '202':
          description: Transfer job accepted and started
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TransferResponse'
        '400':
          description: Invalid request (missing required fields or validation errors)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /transfers/{executionId}:
    get:
      summary: Get transfer job status
      description: Retrieves the current status of a transfer job by its execution ID
      operationId: getTransfer
      parameters:
        - name: executionId
          in: path
          required: true
          description: The job execution ID returned when the transfer was created
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Transfer job found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TransferResponse'
        '404':
          description: Transfer job not found

components:
  schemas:
    CreateTransferRequest:
      type: object
      description: Request to create a file transfer job
      required:
        - targetBaseDir
      properties:
        sourceDir:
          type: string
          description: Source directory for bulk transfer (mutually exclusive with filePath)
          example: "/Volumes/Camera/DCIM/101NZ7_2"
        targetBaseDir:
          type: string
          description: Destination base directory where files will be organized by date
          example: "/Users/anton/Pictures/Imported"
        filePath:
          type: string
          description: Single file path for individual transfer (mutually exclusive with sourceDir)
          example: "/Volumes/Camera/DCIM/101NZ7_2/DSC_2345.JPG"
        extensions:
          type: array
          description: Optional list of file extensions to filter (case-insensitive)
          items:
            type: string
          example: ["jpg", "jpeg", "raw"]

    TransferResponse:
      type: object
      description: Transfer job status information
      required:
        - executionId
        - status
      properties:
        executionId:
          type: integer
          format: int64
          description: Unique job execution ID
          example: 12345
        status:
          type: string
          description: Current job status
          example: "COMPLETED"
          enum:
            - STARTING
            - STARTED
            - STOPPING
            - STOPPED
            - FAILED
            - COMPLETED
            - ABANDONED
            - UNKNOWN
        startTime:
          type: string
          format: date-time
          description: Job start timestamp (ISO 8601)
          example: "2024-01-15T10:30:00Z"
        endTime:
          type: string
          format: date-time
          description: Job end timestamp (ISO 8601)
          example: "2024-01-15T10:35:00Z"

    ErrorResponse:
      type: object
      description: Error response for validation failures
      properties:
        timestamp:
          type: string
          format: date-time
        status:
          type: integer
        error:
          type: string
        message:
          type: string
        path:
          type: string
```

#### Metadata API (`src/main/resources/openapi/metadata-api.yaml`)

```yaml
openapi: 3.0.3
info:
  title: Metadata Analysis API
  description: API for triggering metadata extraction and EXIF analysis
  version: 1.0.0
servers:
  - url: http://localhost:8080
    description: Local development server

paths:
  /api/metadata/analyze:
    post:
      summary: Trigger metadata analysis for a directory
      description: |
        Initiates metadata extraction and EXIF analysis for all supported files
        in the specified directory and its subdirectories.
      operationId: analyzeMetadata
      parameters:
        - name: targetBaseDir
          in: query
          required: true
          description: Base directory to analyze for metadata
          schema:
            type: string
          example: "/Users/anton/Pictures/Imported"
      responses:
        '202':
          description: Metadata analysis triggered successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AnalyzeMetadataResponse'
        '400':
          description: Invalid request (missing or invalid targetBaseDir)

components:
  schemas:
    AnalyzeMetadataResponse:
      type: object
      description: Confirmation that metadata analysis was triggered
      required:
        - message
        - targetBaseDir
      properties:
        message:
          type: string
          description: Confirmation message
          example: "Metadata analysis triggered for base dir: /Users/anton/Pictures/Imported"
        targetBaseDir:
          type: string
          description: Directory being analyzed
          example: "/Users/anton/Pictures/Imported"
```

#### AI API (`src/main/resources/openapi/ai-api.yaml`)

```yaml
openapi: 3.0.3
info:
  title: AI Analysis API
  description: API for triggering AI-based image analysis
  version: 1.0.0
servers:
  - url: http://localhost:8080
    description: Local development server

paths:
  /ai/analyze:
    post:
      summary: Trigger AI analysis for an image
      description: |
        Initiates AI-based analysis for a specific JPG or JPEG image file.
        The analysis results will be added to the image's metadata sidecar file.
      operationId: analyzeImage
      parameters:
        - name: path
          in: query
          required: true
          description: Path to the image file (must be JPG or JPEG)
          schema:
            type: string
            pattern: '.*\.(jpg|jpeg|JPG|JPEG)$'
          example: "/Users/anton/Pictures/Imported/2024/2024-01-15/DSC_2345.JPG"
      responses:
        '200':
          description: AI analysis completed successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AnalyzeAiResponse'
        '202':
          description: AI analysis triggered and will complete asynchronously
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AnalyzeAiResponse'
        '400':
          description: Invalid request (missing path or not a JPG/JPEG file)

components:
  schemas:
    AnalyzeAiResponse:
      type: object
      description: Confirmation that AI analysis was triggered
      required:
        - message
        - path
      properties:
        message:
          type: string
          description: Confirmation message
          example: "AI analysis triggered for: /Users/anton/Pictures/Imported/2024/2024-01-15/DSC_2345.JPG"
        path:
          type: string
          description: Image path being analyzed
          example: "/Users/anton/Pictures/Imported/2024/2024-01-15/DSC_2345.JPG"
```

### Application Configuration

Add to `src/main/resources/application.yaml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operationsSorter: method
    tagsSorter: alpha
```

### Build Process

1. **Clean build**: `./mvnw clean verify`
   - Deletes `target/` directory
   - Runs `generate-sources` phase → generates DTOs from OpenAPI specs
   - Runs `compile` phase → compiles generated DTOs and application code
   - Runs `test` phase → executes unit and property tests
   - Runs `verify` phase → executes integration tests

2. **Incremental build**: `./mvnw compile`
   - Checks if OpenAPI specs changed
   - Regenerates only changed DTOs
   - Compiles modified sources

3. **Generated code location**: `target/generated-sources/openapi/com/ant/filetrans/{module}/api/model/`

4. **IDE integration**:
   - IntelliJ IDEA: Automatically detects generated sources
   - Eclipse: May need to add `target/generated-sources/openapi` as source folder
   - VS Code: Configure Java extension to include generated sources

### Migration Strategy

To migrate from hand-written DTOs to generated DTOs:

1. **Create OpenAPI specifications** for each module
2. **Configure Maven plugin** and run build to generate DTOs
3. **Create mapper classes** to convert between generated DTOs and domain models
4. **Update controllers** to use generated DTOs and mappers
5. **Run tests** to verify backward compatibility
6. **Delete hand-written DTOs** after confirming all tests pass
7. **Update documentation** to reference OpenAPI specs

### Backward Compatibility Verification

To ensure API contracts remain unchanged:

1. **Capture baseline**: Before migration, capture JSON request/response samples from existing endpoints
2. **Compare after migration**: After migration, send same requests and compare JSON responses
3. **Field-by-field comparison**: Verify field names, types, and values match exactly
4. **Status code verification**: Verify HTTP status codes remain the same for success and error cases
5. **Validation behavior**: Verify validation errors produce same error messages

### Development Workflow

1. **Modify OpenAPI spec**: Edit YAML file in `src/main/resources/openapi/`
2. **Regenerate DTOs**: Run `./mvnw generate-sources`
3. **Update mappers**: Adjust mapper logic if DTO structure changed
4. **Update tests**: Add/modify tests for new fields or validation rules
5. **Verify build**: Run `./mvnw clean verify` to ensure everything compiles and tests pass
6. **Test manually**: Use Swagger UI at `http://localhost:8080/swagger-ui.html` to test endpoints
7. **Commit changes**: Commit OpenAPI spec, mapper changes, and test updates

### Troubleshooting

**Problem**: Generated DTOs not found by IDE
- **Solution**: Refresh Maven project, ensure `target/generated-sources/openapi` is marked as source folder

**Problem**: Build fails with "cannot find symbol" for generated DTO
- **Solution**: Run `./mvnw clean generate-sources` to regenerate DTOs

**Problem**: OpenAPI spec has syntax errors
- **Solution**: Use online validator (https://editor.swagger.io/) to validate YAML syntax

**Problem**: Generated DTO missing validation annotations
- **Solution**: Check OpenAPI spec has `required` fields and format constraints defined

**Problem**: Mapper throws exception for valid data
- **Solution**: Check mapper logic handles all DTO field combinations, especially optional fields

**Problem**: Swagger UI not showing endpoints
- **Solution**: Verify `springdoc-openapi-starter-webmvc-ui` dependency is in pom.xml, check application.yaml configuration

## Summary

This design introduces formal OpenAPI specifications and automated code generation for the filetrans REST APIs while maintaining strict modulith boundaries and backward compatibility. The key architectural decisions are:

1. **Separate OpenAPI specs per module** - Each module owns its API contract
2. **Generated DTOs in module-specific packages** - Preserves modulith boundaries
3. **Mapper layer for DTO/domain conversion** - Decouples API contracts from internal models
4. **Maven-based code generation** - Integrates seamlessly with existing build process
5. **Swagger UI for documentation** - Provides interactive API exploration
6. **Comprehensive testing strategy** - Combines unit, property-based, and integration tests

The implementation maintains all existing API contracts while adding formal documentation and type-safe code generation, setting the foundation for future API evolution.
