package org.example.bookstore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.example.bookstore.controllers.catalog.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest( BookController.class )
public class BookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void testGetBooks_whenCatalogIsEmpty() throws Exception {
		// Implement test logic for GET /books endpoint

		mockMvc.perform( get( "/api/v1/books" ).accept( MediaType.APPLICATION_JSON ) )
			.andExpect( status().isOk() )
			.andExpect( content().contentType( MediaType.APPLICATION_JSON ) )
			.andExpect( content().json( "[]" ) );
	}
}
