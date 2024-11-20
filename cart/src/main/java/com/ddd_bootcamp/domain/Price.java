package com.ddd_bootcamp.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Price {
    private final BigDecimal value;
    private final Currency currency;

    @JsonCreator
    public Price(@JsonProperty("value") BigDecimal value,
        @JsonProperty("currency") Currency currency
    ) {
        this.value = value;
        this.currency = currency;
    }

    @JsonProperty("value")
    public BigDecimal getValue() {
        return value;
    }

    @JsonProperty("currency")
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Price price = (Price) o;
        return Objects.equals(value, price.value) &&
                Objects.equals(currency, price.currency);
    }
    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }


    @Override
    public String toString() {
        return "Price{" +
                "value=" + value +
                ", currency=" + currency +
                '}';
    }
}