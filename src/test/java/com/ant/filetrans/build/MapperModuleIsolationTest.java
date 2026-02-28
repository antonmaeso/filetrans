package com.ant.filetrans.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Verifies that mappers respect modulith boundaries and don't import classes from other modules.
 * 
 * Requirements: 8.4, 8.5, 8.6 - Module Boundary Preservation
 */
class MapperModuleIsolationTest {

    private static final String SRC_BASE = "src/main/java/com/ant/filetrans";
    
    // Pattern to match import statements
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+([^;]+);", Pattern.MULTILINE);
    
    @Test
    void transferMapperShouldNotImportFromOtherModules() throws Exception {
        Path transferMapper = Path.of(SRC_BASE, "transfer/web/mapper/TransferApiMapper.java");
        
        assertThat(transferMapper)
                .as("TransferApiMapper should exist")
                .exists();
        
        List<String> imports = extractImports(transferMapper);
        
        // Transfer mapper should not import from metadata or ai modules
        for (String importStatement : imports) {
            assertThat(importStatement)
                    .as("TransferApiMapper should not import from metadata module")
                    .doesNotContain("com.ant.filetrans.metadata");
            
            assertThat(importStatement)
                    .as("TransferApiMapper should not import from ai module")
                    .doesNotContain("com.ant.filetrans.ai");
        }
        
        // Verify it only imports from its own module, generated DTOs, and standard libraries
        for (String importStatement : imports) {
            if (importStatement.startsWith("com.ant.filetrans")) {
                assertThat(importStatement)
                        .as("TransferApiMapper should only import from transfer module or transfer.api.model")
                        .matches("com\\.ant\\.filetrans\\.transfer\\..*");
            }
        }
    }
    
    @Test
    void metadataMapperShouldNotImportFromOtherModules() throws Exception {
        Path metadataMapper = Path.of(SRC_BASE, "metadata/web/mapper/MetadataApiMapper.java");
        
        assertThat(metadataMapper)
                .as("MetadataApiMapper should exist")
                .exists();
        
        List<String> imports = extractImports(metadataMapper);
        
        // Metadata mapper should not import from transfer or ai modules
        for (String importStatement : imports) {
            assertThat(importStatement)
                    .as("MetadataApiMapper should not import from transfer module")
                    .doesNotContain("com.ant.filetrans.transfer");
            
            assertThat(importStatement)
                    .as("MetadataApiMapper should not import from ai module")
                    .doesNotContain("com.ant.filetrans.ai");
        }
        
        // Verify it only imports from its own module, generated DTOs, and standard libraries
        for (String importStatement : imports) {
            if (importStatement.startsWith("com.ant.filetrans")) {
                assertThat(importStatement)
                        .as("MetadataApiMapper should only import from metadata module or metadata.api.model")
                        .matches("com\\.ant\\.filetrans\\.metadata\\..*");
            }
        }
    }
    
    @Test
    void aiMapperShouldNotImportFromOtherModules() throws Exception {
        Path aiMapper = Path.of(SRC_BASE, "ai/web/mapper/AiApiMapper.java");
        
        assertThat(aiMapper)
                .as("AiApiMapper should exist")
                .exists();
        
        List<String> imports = extractImports(aiMapper);
        
        // AI mapper should not import from transfer or metadata modules
        for (String importStatement : imports) {
            assertThat(importStatement)
                    .as("AiApiMapper should not import from transfer module")
                    .doesNotContain("com.ant.filetrans.transfer");
            
            assertThat(importStatement)
                    .as("AiApiMapper should not import from metadata module")
                    .doesNotContain("com.ant.filetrans.metadata");
        }
        
        // Verify it only imports from its own module, generated DTOs, and standard libraries
        for (String importStatement : imports) {
            if (importStatement.startsWith("com.ant.filetrans")) {
                assertThat(importStatement)
                        .as("AiApiMapper should only import from ai module or ai.api.model")
                        .matches("com\\.ant\\.filetrans\\.ai\\..*");
            }
        }
    }
    
    @Test
    void mappersShouldOnlyImportFromTheirOwnModuleOrGeneratedDtos() throws Exception {
        // Find all mapper files
        Path webBase = Path.of(SRC_BASE);
        
        try (Stream<Path> paths = Files.walk(webBase)) {
            List<Path> mapperFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().contains("/web/mapper/"))
                    .filter(p -> p.toString().endsWith("Mapper.java"))
                    .toList();
            
            assertThat(mapperFiles)
                    .as("Should find mapper files")
                    .isNotEmpty();
            
            for (Path mapperFile : mapperFiles) {
                verifyMapperIsolation(mapperFile);
            }
        }
    }
    
    @Test
    void generatedDtosShouldNotHaveCrossModuleDependencies() throws Exception {
        Path generatedBase = Path.of("target/generated-sources/openapi/src/main/java/com/ant/filetrans");
        
        if (!Files.exists(generatedBase)) {
            // If generated sources don't exist yet, skip this test
            return;
        }
        
        // Check each module's generated DTOs
        verifyNoCrossModuleImportsInGeneratedDtos(generatedBase.resolve("transfer/api/model"), "transfer");
        verifyNoCrossModuleImportsInGeneratedDtos(generatedBase.resolve("metadata/api/model"), "metadata");
        verifyNoCrossModuleImportsInGeneratedDtos(generatedBase.resolve("ai/api/model"), "ai");
    }
    
    private void verifyMapperIsolation(Path mapperFile) throws IOException {
        String content = Files.readString(mapperFile);
        List<String> imports = extractImports(mapperFile);
        
        // Determine which module this mapper belongs to
        String moduleName = determineModule(mapperFile);
        
        // Verify no cross-module imports (except events which are allowed)
        for (String importStatement : imports) {
            if (importStatement.startsWith("com.ant.filetrans")) {
                // Allow imports from same module
                // Allow imports from generated api.model packages
                // Disallow imports from other modules (except events)
                if (!importStatement.contains(moduleName) && 
                    !importStatement.contains(".api.model") &&
                    !importStatement.contains(".event")) {
                    
                    assertThat(importStatement)
                            .as("Mapper %s should not import from other modules", mapperFile.getFileName())
                            .contains(moduleName);
                }
            }
        }
    }
    
    private void verifyNoCrossModuleImportsInGeneratedDtos(Path moduleApiPath, String moduleName) throws IOException {
        if (!Files.exists(moduleApiPath)) {
            return;
        }
        
        try (Stream<Path> paths = Files.walk(moduleApiPath)) {
            List<Path> dtoFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            
            for (Path dtoFile : dtoFiles) {
                List<String> imports = extractImports(dtoFile);
                
                for (String importStatement : imports) {
                    if (importStatement.startsWith("com.ant.filetrans")) {
                        // Generated DTOs should not import from other modules
                        assertThat(importStatement)
                                .as("Generated DTO %s should not import from other modules", dtoFile.getFileName())
                                .contains(moduleName);
                    }
                }
            }
        }
    }
    
    private String determineModule(Path mapperFile) {
        String pathStr = mapperFile.toString();
        if (pathStr.contains("/transfer/")) {
            return "transfer";
        } else if (pathStr.contains("/metadata/")) {
            return "metadata";
        } else if (pathStr.contains("/ai/")) {
            return "ai";
        }
        throw new IllegalArgumentException("Cannot determine module for: " + mapperFile);
    }
    
    private List<String> extractImports(Path javaFile) throws IOException {
        String content = Files.readString(javaFile);
        
        return IMPORT_PATTERN.matcher(content)
                .results()
                .map(m -> m.group(1).trim())
                .toList();
    }
}
