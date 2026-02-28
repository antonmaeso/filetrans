package com.ant.filetrans.transfer.web;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests validation error handling and mapper conversion error handling.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MethodParameter methodParameter;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        handler = new GlobalExceptionHandler();
        // Create a real MethodParameter for a dummy method
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class);
        methodParameter = new MethodParameter(method, 0);
    }

    // Dummy method for MethodParameter creation
    @SuppressWarnings("unused")
    private void dummyMethod(String param) {
    }

    @Test
    void shouldReturn400WhenRequiredFieldMissing() {
        // Given: A validation error for a missing required field
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "targetBaseDir", null, false, 
            new String[]{"NotNull"}, null, "must not be null"));
        
        var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        // Then: Returns 400 with field name in message
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
        assertThat(response.getBody().get("field")).isEqualTo("targetBaseDir");
        assertThat(response.getBody().get("message").toString())
            .contains("targetBaseDir")
            .contains("required");
    }

    @Test
    void shouldReturn400WithFieldNameForInvalidFormat() {
        // Given: A validation error for invalid format
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "path", "invalid.txt", false,
            new String[]{"Pattern"}, null, "must match pattern .*\\.(jpg|jpeg|JPG|JPEG)$"));
        
        var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        // Then: Returns 400 with field name and expected format
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("field")).isEqualTo("path");
        assertThat(response.getBody().get("message").toString())
            .contains("path")
            .contains("pattern");
    }

    @Test
    void shouldReturn400WithDescriptiveMessageForPatternMismatch() {
        // Given: A validation error for pattern mismatch
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "extensions", List.of("*.jpg"), false,
            new String[]{"Pattern"}, null, "must not contain wildcards"));
        
        var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        // Then: Returns 400 with pattern description
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message").toString())
            .contains("extensions")
            .contains("wildcards");
    }

    @Test
    void shouldIncludeTimestampInErrorResponse() {
        // Given: Any validation error
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field", null, false,
            null, null, "validation failed"));
        
        var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        // Then: Response includes timestamp
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("timestamp")).isNotNull();
        assertThat(response.getBody().get("timestamp").toString()).matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }

    @Test
    void shouldHandleIllegalArgumentExceptionFromMapper() {
        // Given: An IllegalArgumentException from mapper validation
        var exception = new IllegalArgumentException(
            "Invalid transfer request: either sourceDir or filePath must be provided");

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(exception);

        // Then: Returns 400 with descriptive message
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
        assertThat(response.getBody().get("message"))
            .isEqualTo("Invalid transfer request: either sourceDir or filePath must be provided");
    }

    @Test
    void shouldHandleIllegalArgumentExceptionForInvalidPath() {
        // Given: An IllegalArgumentException for invalid path
        var exception = new IllegalArgumentException(
            "Path must end with .jpg or .jpeg (case-insensitive)");

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(exception);

        // Then: Returns 400 with expected format description
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message").toString())
            .contains("jpg")
            .contains("jpeg");
    }

    @Test
    void shouldHandleMultipleValidationErrors() {
        // Given: Multiple validation errors
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "targetBaseDir", null, false,
            new String[]{"NotNull"}, null, "must not be null"));
        bindingResult.addError(new FieldError("request", "sourceDir", "", false,
            new String[]{"NotBlank"}, null, "must not be blank"));
        
        var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        // Then: Returns 400 with first error (Spring MVC behavior)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("field")).isNotNull();
        assertThat(response.getBody().get("message")).isNotNull();
    }

    @Test
    void shouldIncludeAllRequiredFieldsInErrorResponse() {
        // Given: A validation error
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "targetBaseDir", null, false,
            null, null, "must not be null"));
        
        var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When: Handler processes the exception
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        // Then: Response includes all required fields
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message", "field");
    }
}
