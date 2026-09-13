package org.example.bookstore.dtos.catalog;

import java.util.List;

public record BookPageResponse(List<BookResponse> content, int page, int size, long totalElements, int totalPages, boolean hasNext ) {

}
