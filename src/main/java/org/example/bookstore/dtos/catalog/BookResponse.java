package org.example.bookstore.dtos.catalog;

import java.math.BigDecimal;

public record BookResponse(long id, String title, String author, BigDecimal price, String currency) {

}
