# Requirements Document

## Introduction

This document specifies requirements for implementing OpenAPI specification and code generation for the REST services in the filetrans Spring Boot Modulith application. The system currently has REST endpoints in three modules (transfer, metadata, ai) but lacks formal API documentation and uses hand-written DTOs. This feature will introduce OpenAPI specifications, generate Java DTOs from those specifications, and implement mappers to convert between generated DTOs and internal domain models while maintaining strict modulith boundaries.

## Glossary

- **OpenAPI_Specification**: A YAML or JSON file following the OpenAPI 3.x standard that formally describes REST API endpoints, request/response schemas, and validation rules
- **Generated_DTO**: Data Transfer Object classes automatically generated from OpenAPI schemas using code generation tools
- **Mapper**: A component that converts between Generated_DTOs and internal domain models
- **Transfer_Module**: The modulith module responsible for file transfer operations and batch job management
- **Metadata_Module**: The modulith module responsible for EXIF extraction and metadata catalog operations
- **AI_Module**: The modulith module responsible for AI-based image analysis
- **Module_Boundary**: The architectural separation between modulith modules enforced through package structure and dependency rules
- **Internal_Domain_Model**: Application-specific classes used within module boundaries (e.g., TransferCommand, JobExecution)
- **Code_Generator**: A Maven plugin that generates Java source code from OpenAPI specifications during the build process
- **API_Contract**: The public interface exposed by a module through REST endpoints

## Requirements

### Requirement 1: OpenAPI Specification Creation

**User Story:** As a developer, I want formal OpenAPI specifications for all REST endpoints, so that API contracts are documented and machine-readable

#### Acceptance Criteria

1. THE OpenAPI_Specification SHALL define all endpoints from Transfer_Module (/transfers POST and GET)
2. THE OpenAPI_Specification SHALL define all endpoints from Metadata_Module (/api/metadata/analyze POST)
3. THE OpenAPI_Specification SHALL define all endpoints from AI_Module (/ai/analyze POST)
4. WHERE a module has REST endpoints, THE OpenAPI_Specification SHALL be located within that module's package structure
5. THE OpenAPI_Specification SHALL use OpenAPI version 3.0 or higher
6. THE OpenAPI_Specification SHALL define request schemas with validation constraints (required fields, string formats, patterns)
7. THE OpenAPI_Specification SHALL define response schemas for success and error cases
8. THE OpenAPI_Specification SHALL define HTTP status codes for each endpoint operation
9. FOR ALL existing DTO fields, THE OpenAPI_Specification SHALL include equivalent schema properties

### Requirement 2: Code Generation from OpenAPI

**User Story:** As a developer, I want Java DTOs generated from OpenAPI specifications, so that API contracts and code stay synchronized

#### Acceptance Criteria

1. THE Code_Generator SHALL generate Java classes from OpenAPI_Specification during Maven build
2. THE Code_Generator SHALL place generated classes in target/generated-sources directory
3. THE Code_Generator SHALL generate classes before the compile phase
4. THE Generated_DTO SHALL include Jakarta validation annotations based on OpenAPI constraints
5. THE Generated_DTO SHALL use Java records or immutable classes
6. THE Generated_DTO SHALL include Jackson annotations for JSON serialization
7. WHEN the OpenAPI_Specification changes, THE Code_Generator SHALL regenerate affected classes on next build
8. THE Code_Generator SHALL generate separate packages per module to maintain Module_Boundary
9. THE Generated_DTO SHALL support Java 25 language features

### Requirement 3: DTO to Domain Model Mapping

**User Story:** As a developer, I want mappers between generated DTOs and internal models, so that module internals remain decoupled from API contracts

#### Acceptance Criteria

1. WHERE a Generated_DTO represents external API input, THE Mapper SHALL convert it to Internal_Domain_Model
2. WHERE an Internal_Domain_Model must be returned via API, THE Mapper SHALL convert it to Generated_DTO
3. THE Mapper SHALL handle null values and optional fields according to API contract
4. THE Mapper SHALL perform data transformations (e.g., string to Path, list to Set)
5. THE Mapper SHALL validate business rules not expressible in OpenAPI schema
6. WHEN mapping fails due to invalid data, THE Mapper SHALL throw a descriptive exception
7. THE Mapper SHALL be located within the web layer of its respective module
8. THE Mapper SHALL not expose Internal_Domain_Model classes outside Module_Boundary

### Requirement 4: Transfer Module API Contract

**User Story:** As an API consumer, I want to create and query file transfer jobs, so that I can move files programmatically

#### Acceptance Criteria

1. THE Transfer_Module SHALL expose POST /transfers endpoint accepting CreateTransferRequest schema
2. WHEN a valid transfer request is received, THE Transfer_Module SHALL return 202 Accepted with TransferResponse
3. WHEN a transfer request has missing required fields, THE Transfer_Module SHALL return 400 Bad Request
4. THE Transfer_Module SHALL expose GET /transfers/{executionId} endpoint
5. WHEN a valid executionId is provided, THE Transfer_Module SHALL return 200 OK with TransferResponse
6. WHEN an executionId does not exist, THE Transfer_Module SHALL return 404 Not Found
7. THE CreateTransferRequest schema SHALL support both directory transfer (sourceDir) and single-file transfer (filePath) modes
8. THE CreateTransferRequest schema SHALL include targetBaseDir as required field
9. THE CreateTransferRequest schema SHALL include extensions as optional array of strings
10. THE TransferResponse schema SHALL include executionId, status, startTime, and endTime fields

### Requirement 5: Metadata Module API Contract

**User Story:** As an API consumer, I want to trigger metadata analysis for directories, so that I can extract EXIF data and generate sidecars

#### Acceptance Criteria

1. THE Metadata_Module SHALL expose POST /api/metadata/analyze endpoint
2. WHEN a valid targetBaseDir parameter is provided, THE Metadata_Module SHALL return 202 Accepted
3. WHEN targetBaseDir parameter is missing, THE Metadata_Module SHALL return 400 Bad Request
4. THE Metadata_Module SHALL accept targetBaseDir as a query parameter
5. THE Metadata_Module SHALL return a response body indicating analysis was triggered

### Requirement 6: AI Module API Contract

**User Story:** As an API consumer, I want to trigger AI analysis for specific images, so that I can generate AI metadata on demand

#### Acceptance Criteria

1. THE AI_Module SHALL expose POST /ai/analyze endpoint
2. WHEN a valid path parameter is provided, THE AI_Module SHALL return 200 OK or 202 Accepted
3. WHEN path parameter is missing, THE AI_Module SHALL return 400 Bad Request
4. THE AI_Module SHALL accept path as a query parameter
5. THE AI_Module SHALL validate that path points to a JPG or JPEG file

### Requirement 7: Maven Build Integration

**User Story:** As a developer, I want OpenAPI code generation integrated into the build, so that generated code is always available

#### Acceptance Criteria

1. THE Code_Generator SHALL be configured as a Maven plugin in pom.xml
2. WHEN ./mvnw clean verify is executed, THE Code_Generator SHALL generate all DTOs before compilation
3. WHEN ./mvnw compile is executed, THE Code_Generator SHALL generate DTOs if not present
4. THE Code_Generator SHALL not regenerate DTOs if OpenAPI_Specification is unchanged
5. THE Maven build SHALL fail if OpenAPI_Specification contains syntax errors
6. THE Maven build SHALL fail if generated code has compilation errors
7. THE Code_Generator SHALL be configured to use appropriate Java package names per module

### Requirement 8: Module Boundary Preservation

**User Story:** As an architect, I want API contracts to respect modulith boundaries, so that module independence is maintained

#### Acceptance Criteria

1. THE Generated_DTO for Transfer_Module SHALL reside in com.ant.filetrans.transfer.api package or subpackage
2. THE Generated_DTO for Metadata_Module SHALL reside in com.ant.filetrans.metadata.api package or subpackage
3. THE Generated_DTO for AI_Module SHALL reside in com.ant.filetrans.ai.api package or subpackage
4. THE Mapper SHALL not import classes from other modules except through published events
5. THE OpenAPI_Specification SHALL not define cross-module dependencies in schemas
6. WHERE a module needs to reference another module's data, THE OpenAPI_Specification SHALL use primitive types or duplicate schemas

### Requirement 9: Backward Compatibility

**User Story:** As a developer, I want existing REST endpoints to continue working, so that current API consumers are not broken

#### Acceptance Criteria

1. WHEN Generated_DTOs replace hand-written DTOs, THE API_Contract SHALL remain unchanged
2. THE Generated_DTO field names SHALL match existing DTO field names exactly
3. THE Generated_DTO field types SHALL be compatible with existing DTO field types
4. THE HTTP endpoints paths SHALL remain unchanged
5. THE HTTP methods SHALL remain unchanged
6. THE HTTP status codes SHALL remain unchanged
7. THE request and response JSON structure SHALL remain unchanged

### Requirement 10: Validation and Error Handling

**User Story:** As an API consumer, I want clear validation errors, so that I can correct invalid requests

#### Acceptance Criteria

1. WHEN a request violates OpenAPI constraints, THE system SHALL return 400 Bad Request
2. THE error response SHALL include a message describing which field failed validation
3. THE error response SHALL include the validation rule that was violated
4. WHEN a required field is missing, THE error message SHALL identify the missing field
5. WHEN a field has invalid format, THE error message SHALL describe the expected format
6. THE validation SHALL occur before mapper execution
7. THE validation SHALL be performed by Spring framework based on Jakarta validation annotations

### Requirement 11: OpenAPI Documentation Serving

**User Story:** As an API consumer, I want to view OpenAPI documentation in a browser, so that I can explore available endpoints

#### Acceptance Criteria

1. THE system SHALL serve OpenAPI_Specification at /v3/api-docs endpoint
2. THE system SHALL serve Swagger UI at /swagger-ui.html endpoint
3. WHEN Swagger UI is accessed, THE system SHALL display all documented endpoints
4. THE Swagger UI SHALL allow trying endpoints with sample requests
5. THE OpenAPI_Specification SHALL include descriptions for all endpoints
6. THE OpenAPI_Specification SHALL include descriptions for all schema properties
7. THE OpenAPI_Specification SHALL include example values for request schemas
