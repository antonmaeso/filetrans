/**
 * Validation result interface
 */
export interface ValidationResult {
    valid: boolean;
    error?: string;
}

/**
 * Validate that a path is not empty and has valid format
 * @param path - File system path to validate
 * @param fieldName - Name of the field for error messages (default: 'Path')
 * @returns ValidationResult with valid flag and optional error message
 */
export function validatePath(
    path: string | undefined | null,
    fieldName: string = 'Path'
): ValidationResult {
    if (!path || path.trim() === '') {
        return {
            valid: false,
            error: `${fieldName} is required`,
        };
    }

    const trimmedPath = path.trim();

    // Check for invalid characters (basic validation)
    const invalidChars = /[<>"|?*]/;
    if (invalidChars.test(trimmedPath)) {
        return {
            valid: false,
            error: `${fieldName} contains invalid characters`,
        };
    }

    // Check for path length (reasonable limit)
    if (trimmedPath.length > 4096) {
        return {
            valid: false,
            error: `${fieldName} is too long (max 4096 characters)`,
        };
    }

    return { valid: true };
}

/**
 * Validate file extensions format
 * @param extensions - Array of file extensions or comma-separated string
 * @returns ValidationResult with valid flag and optional error message
 */
export function validateExtensions(
    extensions: string[] | string | undefined | null
): ValidationResult {
    if (!extensions) {
        return { valid: true }; // Extensions are optional
    }

    let extensionArray: string[];

    if (typeof extensions === 'string') {
        if (extensions.trim() === '') {
            return { valid: true }; // Empty string is valid (no filtering)
        }
        extensionArray = extensions.split(',').map(ext => ext.trim());
    } else {
        extensionArray = extensions;
    }

    if (extensionArray.length === 0) {
        return { valid: true }; // Empty array is valid
    }

    // Validate each extension
    for (const ext of extensionArray) {
        if (ext === '') {
            return {
                valid: false,
                error: 'Extensions cannot be empty',
            };
        }

        // Check for valid extension format (alphanumeric, may start with dot)
        const validExtension = /^\.?[a-zA-Z0-9]+$/;
        if (!validExtension.test(ext)) {
            return {
                valid: false,
                error: `Invalid extension format: "${ext}". Use alphanumeric characters only (e.g., "jpg" or ".jpg")`,
            };
        }

        // Check extension length
        if (ext.length > 10) {
            return {
                valid: false,
                error: `Extension "${ext}" is too long (max 10 characters)`,
            };
        }
    }

    return { valid: true };
}

/**
 * Validate that a required field is not empty
 * @param value - Value to validate
 * @param fieldName - Name of the field for error messages
 * @returns ValidationResult with valid flag and optional error message
 */
export function validateRequired(
    value: string | undefined | null,
    fieldName: string
): ValidationResult {
    if (!value || value.trim() === '') {
        return {
            valid: false,
            error: `${fieldName} is required`,
        };
    }

    return { valid: true };
}

/**
 * Validate multiple fields at once
 * @param validations - Array of validation results
 * @returns Combined validation result with all errors
 */
export function combineValidations(validations: ValidationResult[]): ValidationResult {
    const errors = validations
        .filter(v => !v.valid)
        .map(v => v.error)
        .filter((e): e is string => e !== undefined);

    if (errors.length > 0) {
        return {
            valid: false,
            error: errors.join('; '),
        };
    }

    return { valid: true };
}
