package com.project.clariti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import com.project.clariti.Entity.FeeCsvEntity;
import com.project.clariti.Repo.FeeCsvRepository;
import com.project.clariti.utils.Constants;


@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class FeeCalculatorDBServiceTest {
	
    @Mock
    private FeeCsvRepository mockRepo;
    
    @Autowired
    @InjectMocks
    private FeeCalculatorDBService feeCalculatorDBService;

    @Value("${expected.csv.headers}")
    private List<String> expectedCsvHeaders;

    private List<FeeCsvEntity> mockFeeDetails;

    @BeforeEach
    void setUp() {
        // Create mock data
        mockFeeDetails = Arrays.asList(
            createMockFeeCsvEntity("1", "Fee1", "Description1", "Department1", "Category1", "SubCategory1", "Type1", "2", "50"),
            createMockFeeCsvEntity("2", "Fee2", "Description2", "Department1", "Category1", "SubCategory1", "Type1", "3", "30")
        );
    }
    
 // Helper method to create a mock FeeCsvEntity
    private FeeCsvEntity createMockFeeCsvEntity(String id, String name, String description, String department,
            String category, String subCategory, String type, String quantity, String price) {
        FeeCsvEntity entity = new FeeCsvEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription(description);
        entity.setDepartment(department);
        entity.setCategory(category);
        entity.setSubCategory(subCategory);
        entity.setType(type);
        entity.setQuantity(quantity);
        entity.setPrice(price);
        return entity;
    }
    
    @Test
    void testLoadCsvDataIntoDatabase() throws Exception {
        String[] csvHeaderRow = {"Id", "Name", "Description__c", "Department__c", "Category__c", "Sub_Category__c", "Type__c", "Quantity__c", "Unit_Price__c"};
        String[] csvRowData = {"1", "Item1", "Description1", "Dept1", "Category1", "SubCategory1", "Type1", "10", "20.0"};

        StringBuilder csvData = new StringBuilder(String.join(",", csvHeaderRow)).append("\n");
        csvData.append(String.join(",", csvRowData));

        MockMultipartFile csvFile = new MockMultipartFile("file.csv", "file.csv", "text/csv", csvData.toString().getBytes());
        
        when(mockRepo.saveAll(anyList())).thenReturn(mockFeeDetails);

        feeCalculatorDBService.loadCsvDataIntoDatabase(csvFile);

        verify(mockRepo, times(1)).saveAll(anyList());
    }
    
    @Test
	public void testLoadCsvDataIntoDatabaseException() {
    	MultipartFile file = null;
		assertThrows(RuntimeException.class, () -> feeCalculatorDBService.loadCsvDataIntoDatabase(file));
	}

    @Test
    void validateCsvHeadersWithSizeMismatch() throws Exception {
        String[] invalidHeaders = {"InvalidHeader1", "InvalidHeader2"};
        assertThrows(IllegalArgumentException.class, () -> feeCalculatorDBService.validateCsvHeaders(invalidHeaders));
    }
    
    @Test
    void validateCsvHeadersWithNameMismatch() throws Exception {
    	String[] csvHeaderRow = {"ID", "Name", "Description__c", "Department__c", "Category__c", "Sub_Category__c", "Type__c", "Quantity__c", "Unit_Price__c"};
        assertThrows(IllegalArgumentException.class, () -> feeCalculatorDBService.validateCsvHeaders(csvHeaderRow));
    }

    @Test
    void testGetDepartmentFeeFromDbNoDataForFilters() {
        String department = "TestDepartment";
        String category = "TestCategory";
        String subCategory = "TestSubCategory";
        String type = "TestType";

        when(mockRepo.getDepartmentDetails(department, category, subCategory, type)).thenReturn(new ArrayList<>());

        ResponseEntity<Object> responseEntity = feeCalculatorDBService.getDepartmentFeeFromDb(department, category, subCategory, type);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("There is no data for : TestDepartment TestCategory TestSubCategory TestType", responseEntity.getBody());
    }
    
    @Test
    void testGetDepartmentFeeFromDbNoSurcharge() {
        when(mockRepo.getDepartmentDetails(anyString(), anyString(), anyString(), anyString())).thenReturn(mockFeeDetails);

        ResponseEntity<Object> response = feeCalculatorDBService.getDepartmentFeeFromDb("Department1", "Category1", "SubCategory1", "Type1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No surcharge found for department: Department1 only base fee:190", response.getBody()); // Assuming the calculation is correct

        verify(mockRepo, times(1)).getDepartmentDetails("Department1", "Category1", "SubCategory1", "Type1");
    }
    
    @Test
    void testGetDepartmentFeeFromDbWithSurcharge() {
        when(mockRepo.getDepartmentDetails(anyString(), anyString(), anyString(), anyString())).thenReturn(mockFeeDetails);

        ResponseEntity<Object> response = feeCalculatorDBService.getDepartmentFeeFromDb(Constants.MARKETING_DEPARTMENT, "Category1", "SubCategory1", "Type1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(209, response.getBody()); 

        verify(mockRepo, times(1)).getDepartmentDetails(Constants.MARKETING_DEPARTMENT, "Category1", "SubCategory1", "Type1");
    }

    @Test
    void testGetDepartmentFeeFromDbWithEmptyDepartment() {
        ResponseEntity<Object> response = feeCalculatorDBService.getDepartmentFeeFromDb("", "Category1", "SubCategory1", "Type1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Department is empty", response.getBody());

        verifyNoInteractions(mockRepo);
    }
    
    @Test
    void testGetDepartmentFeeFromDbException() {
    	mockFeeDetails = Arrays.asList(createMockFeeCsvEntity("3", "Fee3", "Description3",
				"Department3", "Category3", "SubCategory3", "Type3", "jhj", "50"));
    	when(mockRepo.getDepartmentDetails(anyString(), anyString(), anyString(), anyString())).thenReturn(mockFeeDetails);
    	ResponseEntity<Object> response = feeCalculatorDBService.getDepartmentFeeFromDb("Department1", "Category1", "SubCategory1", "Type1");
    	
    	assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Invalid numeric value: For input string: \"jhj\"", response.getBody());

    }


    @Test
    void testGetBaseFee() {
        String department = "TestDepartment";
        String category = "TestCategory";
        String subCategory = "TestSubCategory";
        String type = "TestType";
        
        when(mockRepo.getDepartmentDetails(department, category, subCategory, type)).thenReturn(mockFeeDetails);

        Float baseFee = feeCalculatorDBService.getBaseFee(department, category, subCategory, type);

        assertEquals(190.0f, baseFee);
    }
    
    @Test
    void testGetBaseFeeException() {
        String department = "TestDepartment";
        String category = "TestCategory";
        String subCategory = "TestSubCategory";
        String type = "TestType";
        
        mockFeeDetails = Arrays.asList(createMockFeeCsvEntity("3", "Fee3", "Description3",
        				"Department3", "Category3", "SubCategory3", "Type3", "jhj", "50"));
        when(mockRepo.getDepartmentDetails(department, category, subCategory, type)).thenReturn(mockFeeDetails);

        assertThrows(RuntimeException.class, () ->
        	feeCalculatorDBService.getBaseFee(department, category, subCategory, type)
        );
    }
}