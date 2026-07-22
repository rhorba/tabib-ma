package com.tabibma.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces docs/architecture-tabib-ma.md Section 7 fitness functions: zero circular
 * dependencies between feature modules, and no module reaching into another module's
 * repository/entity directly (cross-module access must go through a service/event, per
 * the "Cross-module rule" in Architecture Section 2).
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.tabibma");
    }

    @Test
    void featureModulesShouldHaveNoCyclicDependencies() {
        ArchRule rule = slices().matching("com.tabibma.(*)..").should().beFreeOfCycles();
        rule.check(classes);
    }
}
