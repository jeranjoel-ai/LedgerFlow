package com.ledgerflow.common.money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary value: an exact decimal amount plus an ISO-4217 currency code.
 *
 * <p>Money must NEVER be represented as {@code double}/{@code float} anywhere in LedgerFlow - an
 * ArchUnit rule enforces this across all modules. {@link BigDecimal} avoids binary floating-point
 * rounding errors that are unacceptable when moving real money.
 */
public record Money(BigDecimal amount, Currency currency) {

  private static final int SCALE = 4;

  public Money {
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    amount = amount.setScale(SCALE, java.math.RoundingMode.UNNECESSARY);
  }

  public static Money of(BigDecimal amount, String currencyCode) {
    return new Money(amount, Currency.getInstance(currencyCode));
  }

  public static Money zero(String currencyCode) {
    return of(BigDecimal.ZERO, currencyCode);
  }

  public Money add(Money other) {
    requireSameCurrency(other);
    return new Money(this.amount.add(other.amount), this.currency);
  }

  public Money subtract(Money other) {
    requireSameCurrency(other);
    return new Money(this.amount.subtract(other.amount), this.currency);
  }

  public Money negate() {
    return new Money(this.amount.negate(), this.currency);
  }

  public boolean isNegative() {
    return amount.signum() < 0;
  }

  public boolean isPositive() {
    return amount.signum() > 0;
  }

  private void requireSameCurrency(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Cannot combine different currencies: " + this.currency + " and " + other.currency);
    }
  }
}
