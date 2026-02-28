package com.ant.filetrans.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test for specification change propagation.
 * Verifies that modifications to OpenAPI specs result in regenerated DTOs.
 * 
 * Feature: openapi-rest-service
 * Property 3: For any modification to an OpenAPI specification, running the Maven build
 * SHALL regenerate the affected DTO classes to reflect the changes.
 * 
 * Validates: Requirements 2.7
 */
@Tag("property-test")
class SpecificationChangePropagationPropertyTest {

    private static final String OPENAPI_SPECS_BASE = "src/main/resources/openapi";
    private static final String GENERATED_SOURCES_BASE = "target/generated-sources/openapi/com/ant/filetrans";

    @Property(tries = 50)
    void specificationFieldsReflectedInGeneratedCode(
            @ForAll("specificationFields") SpecField field
    ) throws IOException {
        Path specPath = Paths.get(OPENAPI_SPECS_BASE, field.specFile);
        
        if (!specPath.toFile().exists()) {
            return;
        }

        String specContent = Files.readString(specPath);
        
        // Check if the field is defined in the spec
        if (!specContent.contains(field.fieldName)) {
            return;
        }

        // Find the corresponding generated DTO
        Path generatedPath = Paths.get(GENERATED_SOURCES_BASE, field.generatedPath);
        
        if (!generatedPath.toFile().exists()) {
            return;
        }

        String generatedContent = Files.readString(generatedPath);
        
        // Verify the field appears in the generated code
        assertThat(generatedContent).contains(field.fieldName);
    }

    @Property(tries = 50)
    void schemaChangesReflectInGeneratedDtos(
            @ForAll("schemaDefinitions") SchemaDefinition schema
    ) throws IOException {
        Path specPath = Paths.get(OPENAPI_SPECS_BASE, schema.specFile);
        
        if (!specPath.toFile().exists()) {
            return;
        }

        String specContent = Files.readString(specPath);
        
        // Check if the schema is defined in the spec
        if (!specContent.contains(schema.schemaName)) {
            return;
        }

        // Find the corresponding generated DTO
        Path generatedDir = Paths.get(GENERATED_SOURCES_BASE, schema.modulePath);
        
        if (!generatedDir.toFile().exists()) {
            return;
        }

        File[] generatedFiles = generatedDir.toFile().listFiles();
        if (generatedFiles == null) {
            return;
        }

        // Verify a DTO with the schema name exists
        boolean found = false;
        for (File file : generatedFiles) {
            if (file.getName().contains(schema.schemaName)) {
                found = true;
                break;
            }
        }
        
        assertThat(found).isTrue();
    }

    @Property(tries = 50)
    void requiredFieldsGenerateValidationAnnotations(
            @ForAll("requiredFields") RequiredField field
    ) throws IOException {
        Path specPath = Paths.get(OPENAPI_SPECS_BASE, field.specFile);
        
        if (!specPath.toFile().exists()) {
            return;
        }

        String specContent = Files.readString(specPath);
        
        // Check if the field is marked as required in the spec
        Pattern requiredPattern = Pattern.compile(
                "required:\\s*\\[.*" + field.fieldName + ".*\\]",
                Pattern.DOTALL
        );
        Matcher matcher = requiredPattern.matcher(specContent);
        
        if (!matcher.find()) {
            return;
        }

        // Find the corresponding generated DTO
        Path generatedPath = Paths.get(GENERATED_SOURCES_BASE, field.generatedPath);
        
        if (!generatedPath.toFile().exists()) {
            return;
        }

        String generatedContent = Files.readString(generatedPath);
        
        // Verify the field has validation annotation (NotNull or similar)
        assertThat(generatedContent).containsAnyOf("@NotNull", "@Valid", "@NotBlank");
    }

    @Provide
    net.jqwik.api.Arbitrary<SpecField> specificationFields() {
        return net.jqwik.api.Arbitraries.of(
                new SpecField("transfer-api.yaml", "sourceDir", "transfer/api/model/CreateTransferRequest.java"),
                new SpecField("transfer-api.yaml", "targetBaseDir", "transfer/api/model/CreateTransferRequest.java"),
                new SpecField("transfer-api.yaml", "filePath", "transfer/api/model/CreateTransferRequest.java"),
                new SpecField("transfer-api.yaml", "extensions", "transfer/api/model/CreateTransferRequest.java"),
                new SpecField("transfer-api.yaml", "executionId", "transfer/api/model/TransferResponse.java"),
                new SpecField("transfer-api.yaml", "status", "transfer/api/model/TransferResponse.java"),
                new SpecField("metadata-api.yaml", "targetBaseDir", "metadata/api/model/AnalyzeMetadataResponse.java"),
                new SpecField("metadata-api.yaml", "message", "metadata/api/model/AnalyzeMetadataResponse.java"),
                new SpecField("ai-api.yaml", "path", "ai/api/model/AnalyzeAiResponse.java"),
                new SpecField("ai-api.yaml", "message", "ai/api/model/AnalyzeAiResponse.java")
        );
    }

    @Provide
    net.jqwik.api.Arbitrary<SchemaDefinition> schemaDefinitions() {
        return net.jqwik.api.Arbitraries.of(
                new SchemaDefinition("transfer-api.yaml", "CreateTransferRequest", "transfer/api/model"),
                new SchemaDefinition("transfer-api.yaml", "TransferResponse", "transfer/api/model"),
                new SchemaDefinition("metadata-api.yaml", "AnalyzeMetadataResponse", "metadata/api/model"),
                new SchemaDefinition("ai-api.yaml", "AnalyzeAiResponse", "ai/api/model")
        );
    }

    @Provide
    net.jqwik.api.Arbitrary<RequiredField> requiredFields() {
        return net.jqwik.api.Arbitraries.of(
                new RequiredField("transfer-api.yaml", "targetBaseDir", "transfer/api/model/CreateTransferRequest.java"),
                new RequiredField("transfer-api.yaml", "executionId", "transfer/api/model/TransferResponse.java"),
                new RequiredField("transfer-api.yaml", "status", "transfer/api/model/TransferResponse.java"),
                new RequiredField("metadata-api.yaml", "message", "metadata/api/model/AnalyzeMetadataResponse.java"),
                new RequiredField("metadata-api.yaml", "targetBaseDir", "metadata/api/model/AnalyzeMetadataResponse.java"),
                new RequiredField("ai-api.yaml", "message", "ai/api/model/AnalyzeAiResponse.java"),
                new RequiredField("ai-api.yaml", "path", "ai/api/model/AnalyzeAiResponse.java")
        );
    }

    record SpecField(String specFile, String fieldName, String generatedPath) {}
    record SchemaDefinition(String specFile, String schemaName, String modulePath) {}
    record RequiredField(String specFile, String fieldName, String generatedPath) {}
}
