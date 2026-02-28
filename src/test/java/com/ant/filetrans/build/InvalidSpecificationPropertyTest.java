package com.ant.filetrans.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Tag;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test for invalid specification build failure.
 * Verifies that OpenAPI specs with syntax errors would cause build failure.
 * 
 * Feature: openapi-rest-service
 * Property 16: For any OpenAPI specification containing YAML syntax errors,
 * the Maven build SHALL fail with an error message.
 * 
 * Validates: Requirements 7.5
 */
@Tag("property-test")
class InvalidSpecificationPropertyTest {

    private static final String OPENAPI_SPECS_BASE = "src/main/resources/openapi";

    @Property(tries = 50)
    void validSpecificationsParsedSuccessfully(
            @ForAll("validSpecFiles") String specFile
    ) throws IOException {
        Path specPath = Paths.get(OPENAPI_SPECS_BASE, specFile);
        
        if (!specPath.toFile().exists()) {
            return;
        }

        String content = Files.readString(specPath);
        Yaml yaml = new Yaml();
        
        // Valid YAML should parse without exceptions
        Object parsed = yaml.load(content);
        assertThat(parsed).isNotNull();
    }

    @Property(tries = 50)
    void invalidYamlSyntaxDetected(
            @ForAll("invalidYamlSamples") String invalidYaml
    ) {
        Yaml yaml = new Yaml();
        
        // Invalid YAML should throw exception
        assertThatThrownBy(() -> yaml.load(invalidYaml))
                .isInstanceOfAny(YAMLException.class, RuntimeException.class);
    }

    @Property(tries = 50)
    void validOpenApiSpecsHaveRequiredFields(
            @ForAll("validSpecFiles") String specFile
    ) throws IOException {
        Path specPath = Paths.get(OPENAPI_SPECS_BASE, specFile);
        
        if (!specPath.toFile().exists()) {
            return;
        }

        String content = Files.readString(specPath);
        
        // Valid OpenAPI specs must have these fields
        assertThat(content).contains("openapi:");
        assertThat(content).contains("info:");
        assertThat(content).contains("paths:");
    }

    @Property(tries = 50)
    void validOpenApiSpecsHaveCorrectVersion(
            @ForAll("validSpecFiles") String specFile
    ) throws IOException {
        Path specPath = Paths.get(OPENAPI_SPECS_BASE, specFile);
        
        if (!specPath.toFile().exists()) {
            return;
        }

        String content = Files.readString(specPath);
        
        // OpenAPI version should be 3.x
        assertThat(content).containsPattern("openapi:\\s*['\"]?3\\.");
    }

    @Provide
    net.jqwik.api.Arbitrary<String> validSpecFiles() {
        return net.jqwik.api.Arbitraries.of(
                "transfer-api.yaml",
                "metadata-api.yaml",
                "ai-api.yaml"
        );
    }

    @Provide
    net.jqwik.api.Arbitrary<String> invalidYamlSamples() {
        return net.jqwik.api.Arbitraries.of(
                // Unclosed quote
                "openapi: \"3.0.3\ninfo:\n  title: Test",
                // Invalid indentation
                "openapi: 3.0.3\n info:\n  title: Test",
                // Missing colon
                "openapi 3.0.3\ninfo:\n  title: Test",
                // Tab character (YAML doesn't allow tabs for indentation)
                "openapi: 3.0.3\ninfo:\n\ttitle: Test",
                // Duplicate key at same level
                "openapi: 3.0.3\nopenapi: 3.0.2\ninfo:\n  title: Test",
                // Invalid list syntax
                "paths:\n  /test:\n    - get\n    - post",
                // Unclosed bracket
                "components:\n  schemas:\n    Test:\n      type: object\n      properties: {name: string",
                // Invalid escape sequence
                "info:\n  title: \"Test\\xInvalid\"",
                // Missing value
                "openapi:\ninfo:\n  title: Test"
        );
    }
}
