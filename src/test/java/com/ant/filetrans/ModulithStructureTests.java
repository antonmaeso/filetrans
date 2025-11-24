package com.ant.filetrans;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTests {

    ApplicationModules modules = ApplicationModules.of("com.ant.filetrans");

    @Test
    void modulesShouldBeFreeOfCycles() {
        modules.verify();
    }

    @Test
    void printModules() {
        modules.forEach(m -> System.out.println(m.getName()));
    }
}
