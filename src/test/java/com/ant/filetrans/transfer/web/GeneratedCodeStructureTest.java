package com.ant.filetrans.transfer.web;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.ant.filetrans.ai.api.model.AnalyzeAiResponse;
import com.ant.filetrans.metadata.api.model.AnalyzeMetadataResponse;
import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/**
 * Unit tests verifying the structure of generated DTOs from OpenAPI specifications.
 * 
 * Validates Requirements 2.4, 2.5, 2.6, 8.8:
 * - Generated DTOs exist in expected packages
 * - DTOs have Jakarta validation annotations
 * - DTOs have Jackson annotations for JSON serialization
 */
class GeneratedCodeStructureTest {

    @Test
    void transferModuleDtosShouldExistInExpectedPackage() {
        // Verify Transfer module DTOs are in com.ant.filetrans.transfer.api.model package
        assertThat(CreateTransferRequest.class.getPackageName())
            .isEqualTo("com.ant.filetrans.transfer.api.model");
        
        assertThat(TransferResponse.class.getPackageName())
            .isEqualTo("com.ant.filetrans.transfer.api.model");
    }

    @Test
    void metadataModuleDtosShouldExistInExpectedPackage() {
        // Verify Metadata module DTOs are in com.ant.filetrans.metadata.api.model package
        assertThat(AnalyzeMetadataResponse.class.getPackageName())
            .isEqualTo("com.ant.filetrans.metadata.api.model");
    }

    @Test
    void aiModuleDtosShouldExistInExpectedPackage() {
        // Verify AI module DTOs are in com.ant.filetrans.ai.api.model package
        assertThat(AnalyzeAiResponse.class.getPackageName())
            .isEqualTo("com.ant.filetrans.ai.api.model");
    }

    @Test
    void createTransferRequestShouldHaveValidationAnnotations() {
        // Verify required field getter has @NotNull annotation
        Method targetBaseDirGetter = findGetter(CreateTransferRequest.class, "getTargetBaseDir");
        assertThat(targetBaseDirGetter).isNotNull();
        assertThat(targetBaseDirGetter.isAnnotationPresent(NotNull.class))
            .as("getTargetBaseDir() should have @NotNull annotation")
            .isTrue();
    }

    @Test
    void createTransferRequestShouldHaveJacksonAnnotations() {
        // Verify getter methods have @JsonProperty annotations
        Method targetBaseDirGetter = findGetter(CreateTransferRequest.class, "getTargetBaseDir");
        assertThat(targetBaseDirGetter).isNotNull();
        assertThat(targetBaseDirGetter.isAnnotationPresent(JsonProperty.class))
            .as("getTargetBaseDir() should have @JsonProperty annotation")
            .isTrue();
    }

    @Test
    void transferResponseShouldHaveJacksonAnnotations() {
        // Verify TransferResponse has Jackson annotations on getters
        Method executionIdGetter = findGetter(TransferResponse.class, "getExecutionId");
        assertThat(executionIdGetter).isNotNull();
        assertThat(executionIdGetter.isAnnotationPresent(JsonProperty.class))
            .as("getExecutionId() should have @JsonProperty annotation")
            .isTrue();
        
        Method statusGetter = findGetter(TransferResponse.class, "getStatus");
        assertThat(statusGetter).isNotNull();
        assertThat(statusGetter.isAnnotationPresent(JsonProperty.class))
            .as("getStatus() should have @JsonProperty annotation")
            .isTrue();
    }

    @Test
    void analyzeMetadataResponseShouldHaveValidationAnnotations() {
        // Verify required field getters have @NotNull annotation
        Method messageGetter = findGetter(AnalyzeMetadataResponse.class, "getMessage");
        assertThat(messageGetter).isNotNull();
        assertThat(messageGetter.isAnnotationPresent(NotNull.class))
            .as("getMessage() should have @NotNull annotation")
            .isTrue();
        
        Method targetBaseDirGetter = findGetter(AnalyzeMetadataResponse.class, "getTargetBaseDir");
        assertThat(targetBaseDirGetter).isNotNull();
        assertThat(targetBaseDirGetter.isAnnotationPresent(NotNull.class))
            .as("getTargetBaseDir() should have @NotNull annotation")
            .isTrue();
    }

    @Test
    void analyzeMetadataResponseShouldHaveJacksonAnnotations() {
        // Verify AnalyzeMetadataResponse has Jackson annotations on getters
        Method messageGetter = findGetter(AnalyzeMetadataResponse.class, "getMessage");
        assertThat(messageGetter).isNotNull();
        assertThat(messageGetter.isAnnotationPresent(JsonProperty.class))
            .as("getMessage() should have @JsonProperty annotation")
            .isTrue();
    }

    @Test
    void analyzeAiResponseShouldHaveValidationAnnotations() {
        // Verify required field getters have @NotNull annotation
        Method messageGetter = findGetter(AnalyzeAiResponse.class, "getMessage");
        assertThat(messageGetter).isNotNull();
        assertThat(messageGetter.isAnnotationPresent(NotNull.class))
            .as("getMessage() should have @NotNull annotation")
            .isTrue();
        
        Method pathGetter = findGetter(AnalyzeAiResponse.class, "getPath");
        assertThat(pathGetter).isNotNull();
        assertThat(pathGetter.isAnnotationPresent(NotNull.class))
            .as("getPath() should have @NotNull annotation")
            .isTrue();
    }

    @Test
    void analyzeAiResponseShouldHaveJacksonAnnotations() {
        // Verify AnalyzeAiResponse has Jackson annotations on getters
        Method pathGetter = findGetter(AnalyzeAiResponse.class, "getPath");
        assertThat(pathGetter).isNotNull();
        assertThat(pathGetter.isAnnotationPresent(JsonProperty.class))
            .as("getPath() should have @JsonProperty annotation")
            .isTrue();
    }

    @Test
    void generatedDtosShouldHaveGettersAndSetters() {
        // Verify DTOs have standard getter/setter methods
        assertThat(findGetter(CreateTransferRequest.class, "getSourceDir")).isNotNull();
        assertThat(findSetter(CreateTransferRequest.class, "setSourceDir")).isNotNull();
        
        assertThat(findGetter(TransferResponse.class, "getExecutionId")).isNotNull();
        assertThat(findSetter(TransferResponse.class, "setExecutionId")).isNotNull();
        
        assertThat(findGetter(AnalyzeMetadataResponse.class, "getMessage")).isNotNull();
        assertThat(findSetter(AnalyzeMetadataResponse.class, "setMessage")).isNotNull();
        
        assertThat(findGetter(AnalyzeAiResponse.class, "getPath")).isNotNull();
        assertThat(findSetter(AnalyzeAiResponse.class, "setPath")).isNotNull();
    }

    @Test
    void generatedDtosShouldHaveEqualsAndHashCode() {
        // Verify DTOs override equals() and hashCode()
        assertThat(hasOverriddenEquals(CreateTransferRequest.class)).isTrue();
        assertThat(hasOverriddenHashCode(CreateTransferRequest.class)).isTrue();
        
        assertThat(hasOverriddenEquals(TransferResponse.class)).isTrue();
        assertThat(hasOverriddenHashCode(TransferResponse.class)).isTrue();
        
        assertThat(hasOverriddenEquals(AnalyzeMetadataResponse.class)).isTrue();
        assertThat(hasOverriddenHashCode(AnalyzeMetadataResponse.class)).isTrue();
        
        assertThat(hasOverriddenEquals(AnalyzeAiResponse.class)).isTrue();
        assertThat(hasOverriddenHashCode(AnalyzeAiResponse.class)).isTrue();
    }

    // Helper methods

    private Method findGetter(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Method findSetter(Class<?> clazz, String methodName) {
        try {
            // Find setter by name pattern - setters take one parameter
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    return method;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasOverriddenEquals(Class<?> clazz) {
        try {
            Method equals = clazz.getMethod("equals", Object.class);
            return !equals.getDeclaringClass().equals(Object.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean hasOverriddenHashCode(Class<?> clazz) {
        try {
            Method hashCode = clazz.getMethod("hashCode");
            return !hashCode.getDeclaringClass().equals(Object.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
