package com.ledgerflow.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void shouldScaleAmountToFourDecimalPlaces() {
    Money money = Money.of(new BigDecimal("10.5"), "USD");

    assertThat(money.amount()).isEqualByComparingTo("10.5000");
  }

  @Test
  void shouldAddTwoMoneyValuesInSameCurrency() {
    Money ten = Money.of(new BigDecimal("10.00"), "USD");
    Money five = Money.of(new BigDecimal("5.00"), "USD");

    assertThat(ten.add(five).amount()).isEqualByComparingTo("15.0000");
  }

  @Test
  void shouldThrowWhenAddingDifferentCurrencies() {
    Money usd = Money.of(BigDecimal.TEN, "USD");
    Money eur = Money.of(BigDecimal.TEN, "EUR");

    assertThatThrownBy(() -> usd.add(eur)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldDetectNegativeAmount() {
    Money negative = Money.of(new BigDecimal("-5.00"), "USD");

    assertThat(negative.isNegative()).isTrue();
    assertThat(negative.isPositive()).isFalse();
  }

  @Test
  void zeroShouldHaveZeroAmount() {
    Money zero = Money.zero("USD");

    assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
