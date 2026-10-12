package com.ledgerflow.architecture.fixtures;

/**
 * Deliberately violates the money-safety rule (double amount field). Used only by {@link
 * com.ledgerflow.architecture.NoDoubleOrFloatAmountFieldTest} to prove the ArchUnit rule actually
 * fails on a real violation, not just passes vacuously. Must stay out of the "com.ledgerflow"
 * package scanned against production code, or be excluded explicitly in the test.
 */
public class BadMoneyUsageFixture {

  @SuppressWarnings("unused")
  private double amount;
}
