package com.ant.filetrans.transfer.web;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.yaml.snakeyaml.Yaml;

import com.ant.filetrans.transfer.web.dto.CreateTransferRequest;
import com.ant.filetrans.transfer.web.dto.TransferResponse;

import net.jqwik.api.Label;
import net.jqwik.api.Property;

/**
 * Property-based test for DTO field completeness.
 *
 * **Validates: Requirements 1.9**
 *
 * Property 1: DTO Field Completeness - For any existing hand-written DTO field,
 * the OpenAPI schema SHALL include an equivalent property with the same name and compatible type.
 */
class DtoFieldCompletenessPropertyTest {

    private static final String OPENAPI_SPEC_PATH = "/openapi/transfer-api.yaml";

    @Property(tries = 100)
    @Label("CreateTransferRequest fields must exist in OpenAPI schema")
    void createTransferRequestFieldsExistInOpenApiSchema() {
        // Load OpenAPI spec
        Map<String, Object> openApiSpec = loadOpenApiSpec();

        // Get the schema from OpenAPI spec
        Map<String, Object> schema = getSchemaFromSpec(openApiSpec, "CreateTransferRequest");

        // Get all fields from the hand-written DTO
        Field[] fields = CreateTransferRequest.class.getDeclaredFields();

        // Verify each field exists in the OpenAPI schema with compatible type
        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            assertThat(schema)
                    .as("OpenAPI schema should contain field: %s", fieldName)
                    .containsKey(fieldName);

            // Verify type compatibility
            Map<String, Object> fieldSchema = getFieldSchema(schema, fieldName);
            assertTypeCompatibility(fieldName, fieldType, fieldSchema);
        }
    }

    @Property(tries = 100)
    @Label("TransferResponse fields must exist in OpenAPI schema")
    void transferResponseFieldsExistInOpenApiSchema() {
        // Load OpenAPI spec
        Map<String, Object> openApiSpec = loadOpenApiSpec();

        // Get the schema from OpenAPI spec
        Map<String, Object> schema = getSchemaFromSpec(openApiSpec, "TransferResponse");

        // Get all fields from the hand-written DTO
        Field[] fields = TransferResponse.class.getDeclaredFields();

        // Verify each field exists in the OpenAPI schema with compatible type
        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            assertThat(schema)
                    .as("OpenAPI schema should contain field: %s", fieldName)
                    .containsKey(fieldName);

            // Verify type compatibility
            Map<String, Object> fieldSchema = getFieldSchema(schema, fieldName);
            assertTypeCompatibility(fieldName, fieldType, fieldSchema);
        }
    }

    private Map<String, Object> loadOpenApiSpec() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getResourceAsStream(OPENAPI_SPEC_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("OpenAPI spec not found at: " + OPENAPI_SPEC_PATH);
            }
            return yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load OpenAPI spec", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSchemaFromSpec(Map<String, Object> openApiSpec, String schemaName) {
        Map<String, Object> components = (Map<String, Object>) openApiSpec.get("components");
        assertThat(components).isNotNull();

        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertThat(schemas).isNotNull();

        Map<String, Object> schema = (Map<String, Object>) schemas.get(schemaName);
        assertThat(schema)
                .as("Schema %s should exist in OpenAPI spec", schemaName)
                .isNotNull();

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties)
                .as("Schema %s should have properties", schemaName)
                .isNotNull();

        return properties;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFieldSchema(Map<String, Object> properties, String fieldName) {
        Object fieldSchema = properties.get(fieldName);
        assertThat(fieldSchema)
                .as("Field %s should have a schema definition", fieldName)
                .isInstanceOf(Map.class);
        return (Map<String, Object>) fieldSchema;
    }

    private void assertTypeCompatibility(String fieldName, Class<?> javaType, Map<String, Object> openApiSchema) {
        String openApiType = (String) openApiSchema.get("type");
        String openApiFormat = (String) openApiSchema.get("format");

        // Type mapping validation
        if (javaType == String.class) {
            assertThat(openApiType)
                    .as("Field %s: String should map to 'string' type", fieldName)
                    .isEqualTo("string");
        } else if (javaType == Long.class || javaType == long.class) {
            assertThat(openApiType)
                    .as("Field %s: Long should map to 'integer' type", fieldName)
                    .isEqualTo("integer");
            assertThat(openApiFormat)
                    .as("Field %s: Long should have 'int64' format", fieldName)
                    .isEqualTo("int64");
        } else if (javaType == Integer.class || javaType == int.class) {
            assertThat(openApiType)
                    .as("Field %s: Integer should map to 'integer' type", fieldName)
                    .isEqualTo("integer");
        } else if (javaType == Instant.class) {
            assertThat(openApiType)
                    .as("Field %s: Instant should map to 'string' type", fieldName)
                    .isEqualTo("string");
            assertThat(openApiFormat)
                    .as("Field %s: Instant should have 'date-time' format", fieldName)
                    .isEqualTo("date-time");
        } else if (javaType == List.class || javaType.isArray()) {
            assertThat(openApiType)
                    .as("Field %s: List/Array should map to 'array' type", fieldName)
                    .isEqualTo("array");
        }
    }
}
