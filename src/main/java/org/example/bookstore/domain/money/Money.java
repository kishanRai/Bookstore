package org.example.bookstore.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Exact, immutable monetary value. Fractional cents are rejected rather than rounded silently. */
public record Money(BigDecimal amount, CurrencyCode currency) {
    /**
     * Creates a nonnegative monetary value with exact two-decimal precision.
     *
     * @param amount exact monetary amount
     * @param currency supported currency code
     * @throws IllegalArgumentException if the amount is negative
     * @throws ArithmeticException if the amount contains fractional cents
     */
    public Money {
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
    /**
     * Creates an exact zero monetary amount in the supported currency.
     *
     * @return zero EUR with scale two
     */
    public static Money zero() { return new Money(BigDecimal.ZERO, CurrencyCode.EUR); }
    /**
     * Adds an amount in the same currency without changing either operand.
     *
     * @param other amount in the same currency
     * @return the exact sum
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money add(Money other) {
        if (currency != other.currency) throw new IllegalArgumentException("Currencies must match");
        return new Money(amount.add(other.amount), currency);
    }
    /**
     * Multiplies the amount by a nonnegative copy count without rounding.
     *
     * @param quantity requested copy count
     * @return the exact product
     * @throws IllegalArgumentException if the multiplier is negative
     */
    public Money multiply(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
}
