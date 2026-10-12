package com.ledgerflow.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerflow.architecture.fixtures.BadMoneyUsageFixture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.lang.syntax.elements.GivenFieldsConjunction;
import org.junit.jupiter.api.Test;

/**
 * Enforces the single most important money-safety rule in LedgerFlow: amounts must be {@link
 * java.math.BigDecimal}, never binary floating-point. A {@code double}/{@code float} "amount" field
 * would silently introduce rounding errors that are unacceptable when moving real money.
 */
class NoDoubleOrFloatAmountFieldTest {

  private static final JavaClasses ALL_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ledgerflow");

  @Test
  void amountFieldsMustNotBeDoubleOrFloat() {
    rule().check(ALL_CLASSES);
  }

  @Test
  void shouldFailWhenAFieldNamedAmountIsADouble() {
    JavaClasses fixtureOnly = new ClassFileImporter().importClasses(BadMoneyUsageFixture.class);

    assertThatThrownBy(() -> rule().check(fixtureOnly)).isInstanceOf(AssertionError.class);
  }

  private static ArchRule rule() {
    GivenFieldsConjunction fieldsNamedAmount =
        ArchRuleDefinition.fields().that().haveNameMatching("(?i).*amount.*");

    return fieldsNamedAmount
        .should()
        .notHaveRawType(double.class)
        .andShould()
        .notHaveRawType(Double.class)
        .andShould()
        .notHaveRawType(float.class)
        .andShould()
        .notHaveRawType(Float.class)
        .because(
            "money must be represented as BigDecimal with a currency code, never"
                + " double/float - see docs/SPEC.md money-safety requirements");
  }
}
