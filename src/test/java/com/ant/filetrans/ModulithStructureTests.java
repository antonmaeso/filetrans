package com.ant.filetrans;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import lombok.extern.slf4j.Slf4j;

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
}
