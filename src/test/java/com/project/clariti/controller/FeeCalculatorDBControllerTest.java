package com.project.clariti.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;

import com.project.clariti.service.FeeCalculatorDBService;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class FeeCalculatorDBControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Mock
	private FeeCalculatorDBService service;

	@InjectMocks
	private FeeCalculatorDBController controller;

	@Test
	public void testCalculateFee() throws Exception {

		// Performing the request and validating the response
		mockMvc.perform(MockMvcRequestBuilders.get("/calculateFeeFromDB")
				.param("department", "IT")
				.param("category", "")
				.param("subCategory", "")
				.param("type", "")
				.contentType(MediaType.APPLICATION_JSON))
		.andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
		.andExpect(MockMvcResultMatchers.content().string("There is no data for : IT"));

	}


	@Test
	void testGetDepartmentFeeFromDb() {
		String department = "TestDepartment";
		String category = "TestCategory";
		String subCategory = "TestSubCategory";
		String type = "TestType";
		ResponseEntity<Object> expectedResponse = new ResponseEntity<>("TestResponse", HttpStatus.OK);

		when(service.getDepartmentFeeFromDb(department, category, subCategory, type)).thenReturn(expectedResponse);

		ResponseEntity<Object> actualResponse = controller.getDepartmentFee(department, category, subCategory, type);

		assertEquals(expectedResponse, actualResponse);

		verify(service).getDepartmentFeeFromDb(department, category, subCategory, type);
	}

	@Test
	void testUploadCsv() throws IOException {
		InputStream inputStream = new ByteArrayInputStream("test,csv,data".getBytes());
		MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", inputStream);
		ResponseEntity<String> expectedResponse = ResponseEntity.status(HttpStatus.OK)
				.body("CSV file uploaded and data processed successfully.");

		doNothing().when(service).loadCsvDataIntoDatabase(file);

		ResponseEntity<String> actualResponse = controller.uploadCsv(file);

		assertEquals(expectedResponse, actualResponse);
		verify(service).loadCsvDataIntoDatabase(file);
	}
	
	@Test
    void testUploadCsv_Failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "csv,data\n1,test".getBytes());

        doThrow(new RuntimeException("check csv exception")).when(service).loadCsvDataIntoDatabase(file);

        ResponseEntity<String> response = controller.uploadCsv(file);

        verify(service, times(1)).loadCsvDataIntoDatabase(file);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error processing CSV file: check csv exception", response.getBody());
    }
	
}