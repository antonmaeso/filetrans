package com.ant.filetrans.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Verifies that generated DTOs respect modulith boundaries and are placed in correct packages.
 * 
 * Requirements: 8.1, 8.2, 8.3 - Module Boundary Preservation
 */
class GeneratedDtoPackageStructureTest {

    private static final String GENERATED_SOURCES_BASE = "target/generated-sources/openapi/src/main/java";
    
    @Test
    void transferDtosShouldBeInTransferApiModelPackage() throws Exception {
        Path transferApiModelPath = Path.of(GENERATED_SOURCES_BASE, 
                "com/ant/filetrans/transfer/api/model");
        
        assertThat(transferApiModelPath)
                .as("Transfer module DTOs should be in com.ant.filetrans.transfer.api.model package")
                .exists()
                .isDirectory();
        
        // Verify expected DTOs exist
        assertThat(transferApiModelPath.resolve("CreateTransferRequest.java"))
                .as("CreateTransferRequest DTO should exist in transfer.api.model")
                .exists()
                .isRegularFile();
        
        assertThat(transferApiModelPath.resolve("TransferResponse.java"))
                .as("TransferResponse DTO should exist in transfer.api.model")
                .exists()
                .isRegularFile();
        
        // Verify package declaration in generated files
        verifyPackageDeclaration(transferApiModelPath.resolve("CreateTransferRequest.java"),
                "com.ant.filetrans.transfer.api.model");
        verifyPackageDeclaration(transferApiModelPath.resolve("TransferResponse.java"),
                "com.ant.filetrans.transfer.api.model");
    }
    
    @Test
    void metadataDtosShouldBeInMetadataApiModelPackage() throws Exception {
        Path metadataApiModelPath = Path.of(GENERATED_SOURCES_BASE,
                "com/ant/filetrans/metadata/api/model");
        
        assertThat(metadataApiModelPath)
                .as("Metadata module DTOs should be in com.ant.filetrans.metadata.api.model package")
                .exists()
                .isDirectory();
        
        // Verify expected DTOs exist
        assertThat(metadataApiModelPath.resolve("AnalyzeMetadataResponse.java"))
                .as("AnalyzeMetadataResponse DTO should exist in metadata.api.model")
                .exists()
                .isRegularFile();
        
        // Verify package declaration
        verifyPackageDeclaration(metadataApiModelPath.resolve("AnalyzeMetadataResponse.java"),
                "com.ant.filetrans.metadata.api.model");
    }
    
    @Test
    void aiDtosShouldBeInAiApiModelPackage() throws Exception {
        Path aiApiModelPath = Path.of(GENERATED_SOURCES_BASE,
                "com/ant/filetrans/ai/api/model");
        
        assertThat(aiApiModelPath)
                .as("AI module DTOs should be in com.ant.filetrans.ai.api.model package")
                .exists()
                .isDirectory();
        
        // Verify expected DTOs exist
        assertThat(aiApiModelPath.resolve("AnalyzeAiResponse.java"))
                .as("AnalyzeAiResponse DTO should exist in ai.api.model")
                .exists()
                .isRegularFile();
        
        // Verify package declaration
        verifyPackageDeclaration(aiApiModelPath.resolve("AnalyzeAiResponse.java"),
                "com.ant.filetrans.ai.api.model");
    }
    
    @Test
    void generatedDtosShouldNotExistOutsideModulePackages() throws Exception {
        Path generatedBase = Path.of(GENERATED_SOURCES_BASE, "com/ant/filetrans");
        
        if (!Files.exists(generatedBase)) {
            // If generated sources don't exist yet, skip this test
            return;
        }
        
        // Find all Java files in generated sources
        try (Stream<Path> paths = Files.walk(generatedBase)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            
            // Verify each Java file is in one of the expected module packages
            for (Path javaFile : javaFiles) {
                String relativePath = generatedBase.relativize(javaFile).toString();
                
                assertThat(relativePath)
                        .as("Generated DTO %s should be in a module-specific api.model package", javaFile.getFileName())
                        .matches("(transfer|metadata|ai)/api/model/.*\\.java");
            }
        }
    }
    
    @Test
    void eachModuleShouldHaveItsOwnApiModelPackage() {
        // Verify the three modules have separate api.model packages
        Path transferApi = Path.of(GENERATED_SOURCES_BASE, "com/ant/filetrans/transfer/api/model");
        Path metadataApi = Path.of(GENERATED_SOURCES_BASE, "com/ant/filetrans/metadata/api/model");
        Path aiApi = Path.of(GENERATED_SOURCES_BASE, "com/ant/filetrans/ai/api/model");
        
        assertThat(transferApi)
                .as("Transfer module should have its own api.model package")
                .exists()
                .isDirectory();
        
        assertThat(metadataApi)
                .as("Metadata module should have its own api.model package")
                .exists()
                .isDirectory();
        
        assertThat(aiApi)
                .as("AI module should have its own api.model package")
                .exists()
                .isDirectory();
        
        // Verify they are separate directories (not shared)
        assertThat(transferApi).isNotEqualTo(metadataApi);
        assertThat(transferApi).isNotEqualTo(aiApi);
        assertThat(metadataApi).isNotEqualTo(aiApi);
    }
    
    private void verifyPackageDeclaration(Path javaFile, String expectedPackage) throws Exception {
        String content = Files.readString(javaFile);
        String packageDeclaration = "package " + expectedPackage + ";";
        
        assertThat(content)
                .as("File %s should declare package %s", javaFile.getFileName(), expectedPackage)
                .contains(packageDeclaration);
    }
}
