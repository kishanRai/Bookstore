package org.example.bookstore.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import org.example.bookstore.domain.money.CurrencyCode;
import org.example.bookstore.domain.money.Money;
import org.junit.jupiter.api.Test;

/**
 * Verifies exact decimal arithmetic, scale normalization and invalid monetary inputs.
 */
class MoneyTest {
    @Test void calculatesDecimalAmountsExactlyAndNormalizesScale() {
        var tenCents = new Money(new BigDecimal("0.10"), CurrencyCode.EUR);
        assertThat(tenCents.multiply(3).amount()).isEqualTo(new BigDecimal("0.30"));
        assertThat(tenCents.add(new Money(new BigDecimal("0.2"), CurrencyCode.EUR)).amount())
            .isEqualTo(new BigDecimal("0.30"));
        assertThat(new Money(new BigDecimal("1"), CurrencyCode.EUR))
            .isEqualTo(new Money(new BigDecimal("1.00"), CurrencyCode.EUR));
    }
    @Test void rejectsNegativeAmountsFractionalCentsAndNegativeQuantities() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"), CurrencyCode.EUR)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001"), CurrencyCode.EUR)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> Money.zero().multiply(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
