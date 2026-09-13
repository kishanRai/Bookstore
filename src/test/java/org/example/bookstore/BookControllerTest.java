package org.example.bookstore;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.springframework.context.annotation.Import;
import org.example.bookstore.configurations.security.SecurityConfiguration;
import org.example.bookstore.controllers.catalog.BookController;
import org.example.bookstore.dtos.catalog.BookPageResponse;
import org.example.bookstore.services.catalog.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest( BookController.class )
@Import(SecurityConfiguration.class)
class BookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookService bookService;

	@Test
	void testGetBooks_whenCatalogIsEmpty() throws Exception {
		given(bookService.getBooks(0, 20))
			.willReturn(new BookPageResponse(
				List.of(), 0, 20, 0, 0, false
			));

		mockMvc.perform(get("/api/v1/books"))
			.andExpect(status().isOk())
			.andExpect(content().json("""
                    {
                      "content": [],
                      "page": 0,
                      "size": 20,
                      "totalElements": 0,
                      "totalPages": 0,
                      "hasNext": false
                    }
                    """));
	}


	@Test
	void shouldUseDefaultPagination() throws Exception {
		given(bookService.getBooks(0, 20))
			.willReturn(  new BookPageResponse( List.of(), 0, 20, 0, 0, false )  );

		mockMvc.perform(get("/api/v1/books"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty())
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20));

		verify(bookService).getBooks(0, 20);
	}


	@ParameterizedTest
	@CsvSource({
		"0, 1",
		"1, 2",
		"10000, 100"
	})
	void shouldAcceptValidPagination(int page, int size) throws Exception {
		given(bookService.getBooks(page, size))
			.willReturn( new BookPageResponse( List.of(), page, size, 0, 0, false ) );

		mockMvc.perform(get("/api/v1/books")
							.param("page", Integer.toString(page))
							.param("size", Integer.toString(size)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(page))
			.andExpect(jsonPath("$.size").value(size));

		verify(bookService).getBooks(page, size);
	}

	@ParameterizedTest
	@CsvSource({
		"-1, 20",
		"10001, 20",
		"0, 0",
		"0, -1",
		"0, 101",
		"abc, 20",
		"0, abc",
		"0, 1.5",
		"2147483648, 20"
	})
	void shouldRejectInvalidPagination(String page, String size)
		throws Exception {

		mockMvc.perform(get("/api/v1/books")
							.param("page", page)
							.param("size", size))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(bookService);
	}
}
