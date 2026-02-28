package com.ant.filetrans;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies modulith structure and boundaries are respected.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5 - Module Boundary Preservation
 */
@Slf4j
class ModulithStructureTests {

    ApplicationModules modules = ApplicationModules.of("com.ant.filetrans");

    @Test
    void modulesShouldBeFreeOfCycles() {
        modules.verify();
    }

    @Test
    void printModules() {
        modules.forEach(m -> log.info(m.getName()));
    }
    
    @Test
    void shouldHaveThreeMainModules() {
        // Verify the three main feature modules exist
        assertThat(modules.stream()
                .map(ApplicationModule::getName)
                .filter(name -> name.equals("transfer") || name.equals("metadata") || name.equals("ai"))
                .count())
                .as("Should have transfer, metadata, and ai modules")
                .isGreaterThanOrEqualTo(3);
    }
    
    @Test
    void transferModuleShouldNotDependOnMetadataOrAi() {
        modules.getModuleByName("transfer").ifPresent(transferModule -> {
            transferModule.getDependencies(modules).stream().forEach(dep -> {
                String targetModule = dep.getTargetModule().getName();
                assertThat(targetModule)
                        .as("Transfer module should not depend on metadata or ai modules")
                        .isNotIn("metadata", "ai");
            });
        });
    }
    
    @Test
    void metadataModuleShouldOnlyDependOnTransferAndAiForEvents() {
        // Metadata module can depend on transfer and ai modules for event types only
        // This is the correct architecture per AGENTS.md:
        // - Metadata listens to FileTransferredEvent from transfer
        // - Metadata listens to AiAnalysisCompletedEvent from ai
        modules.getModuleByName("metadata").ifPresent(metadataModule -> {
            metadataModule.getDependencies(modules).stream().forEach(dep -> {
                String targetModule = dep.getTargetModule().getName();
                // Metadata can depend on transfer and ai for events - this is expected
                assertThat(targetModule)
                        .as("Metadata module dependencies should be transfer or ai (for events)")
                        .isIn("transfer", "ai");
            });
        });
    }
    
    @Test
    void aiModuleShouldNotDependOnMetadata() {
        modules.getModuleByName("ai").ifPresent(aiModule -> {
            aiModule.getDependencies(modules).stream().forEach(dep -> {
                String targetModule = dep.getTargetModule().getName();
                assertThat(targetModule)
                        .as("AI module should not depend on metadata module")
                        .isNotEqualTo("metadata");
            });
        });
    }
    
    @Test
    void apiPackagesShouldBePartOfModuleBoundaries() {
        // Verify that api.model packages are recognized as part of their respective modules
        modules.getModuleByName("transfer").ifPresent(transferModule -> {
            assertThat(transferModule.getBasePackage().getName())
                    .as("Transfer module should have correct base package")
                    .isEqualTo("com.ant.filetrans.transfer");
        });
        
        modules.getModuleByName("metadata").ifPresent(metadataModule -> {
            assertThat(metadataModule.getBasePackage().getName())
                    .as("Metadata module should have correct base package")
                    .isEqualTo("com.ant.filetrans.metadata");
        });
        
        modules.getModuleByName("ai").ifPresent(aiModule -> {
            assertThat(aiModule.getBasePackage().getName())
                    .as("AI module should have correct base package")
                    .isEqualTo("com.ant.filetrans.ai");
        });
    }
    
    @Test
    void modulesShouldNotHaveCircularDependencies() {
        // This is a more explicit test for circular dependencies
        modules.verify();
        
        // Additionally verify no module depends on itself
        modules.forEach(module -> {
            module.getDependencies(modules).stream().forEach(dep -> {
                assertThat(dep.getTargetModule().getName())
                        .as("Module %s should not depend on itself", module.getName())
                        .isNotEqualTo(module.getName());
            });
        });
    }
}
