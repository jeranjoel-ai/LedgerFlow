package com.ledgerflow.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layering rule from the conventions doc: Controller -> Service -> Repository.
 * Controllers must never call repositories directly, so business rules (validation, security
 * checks, orchestration) can't be bypassed by going straight to persistence from the web layer.
 */
class ControllerRepositoryLayeringTest {

  private static final JavaClasses ALL_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ledgerflow");

  @Test
  void controllersShouldNotDependOnRepositories() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..api..")
            .or()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infra..repository..")
            .orShould()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .because(
                "controllers must go through a service, never call a repository directly"
                    + " (Controller -> Service -> Repository)")
            // No controllers/repositories exist yet (added from Phase 1 onward); this rule
            // guards future code and must not fail just because nothing matches today.
            .allowEmptyShould(true);

    rule.check(ALL_CLASSES);
  }
}
