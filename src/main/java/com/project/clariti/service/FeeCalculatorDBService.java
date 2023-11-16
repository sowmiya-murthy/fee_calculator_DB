package com.project.clariti.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.project.clariti.Entity.FeeCsvEntity;
import com.project.clariti.Repo.FeeCsvRepository;
import com.project.clariti.utils.Constants;

@Service
public class FeeCalculatorDBService {
	
	private static final Logger logger = LogManager.getLogger(FeeCalculatorDBService.class);
	
	@Autowired
	private FeeCsvRepository repo;
	
	@Value("${expected.csv.headers}")
	private List<String> expectedCsvHeaders;
	
	/**
	 * loadCsvDataIntoDatabase method loads the csv data from the file into h2 db
	 * @param file 
	 */
	public void loadCsvDataIntoDatabase(MultipartFile file) {
	    try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))){
	        List<String[]> records = csvReader.readAll();
	        if(validateCsvHeaders(records.get(0))) {
	        	//load the csv data in entity
		        List<FeeCsvEntity> entities = 
		        		records.stream().skip(1).map(columns -> {
		                	FeeCsvEntity entity = new FeeCsvEntity();
		                	entity.setId(columns[Constants.ID_INDEX]);
		                	entity.setName(columns[Constants.NAME_INDEX]);
		                	entity.setDescription(columns[Constants.DESC_INDEX]);
		                    entity.setDepartment(columns[Constants.DEPARTMENT_INDEX]);
		                    entity.setCategory(columns[Constants.CATEGORY_INDEX]);
		                    entity.setSubCategory(columns[Constants.SUBCATEGORY_INDEX]);
		                    entity.setType(columns[Constants.TYPE_INDEX]);
		                    entity.setQuantity(columns[Constants.QUANTITY_INDEX]);
		                    entity.setPrice(columns[Constants.PRICE_INDEX]);
		                    return entity;
		                }).collect(Collectors.toList());

		        // Save entities to the database
		        repo.saveAll(entities);
	        }
	    } catch (IOException | CsvException e) {
	        logger.error("Error reading CSV file or loading data into the database: {}", e.getMessage(), e);
	        throw new RuntimeException("Error reading CSV file or loading data into the database: " + e.getMessage(), e);
	    }
	}
	
	/**
	 * validateCsvHeaders method validates the csv file headers with expected headers
	 * if all presents returns true or throws IllegalArgumentException
	 */
	public boolean validateCsvHeaders(String[] csvHeaders) {

		boolean isValid = false;
        if (csvHeaders.length < expectedCsvHeaders.size()) {
        	isValid = false;
            throw new IllegalArgumentException("Number of headers in CSV do not match the expected headers.");
        }
        
        for (int i = 0; i < csvHeaders.length; i++) {
            String currentHeader = csvHeaders[i].replaceAll("\uFEFF", "");
            if(expectedCsvHeaders.indexOf(currentHeader) != -1) {
            	isValid = true;
            }else {
            	isValid = false;
            	throw new IllegalArgumentException("Unexpected header found in CSV: " + currentHeader);
            }     
        }

        return isValid;
    }

	
	/**
	 * getDepartmentFeeFromDb method calculates the fee based on the filter values and applies 
	 * surcharge if present for the particular department
	 * @param department, category, sub_category, type
	 * @return ResponseEntity
	 */
	public ResponseEntity<Object> getDepartmentFeeFromDb(String department, String category, String subCategory,
			String type) {
		try {
			if(department.isBlank()) {
				return new ResponseEntity<>("Department is empty", HttpStatus.BAD_REQUEST);
			}else {
				// to calculate base fee
				Float baseFee = getBaseFee(department, category, subCategory, type);
				
				if(baseFee == 0) {
					
					String msg = "There is no data for : " +department+" "
							+category+" "+subCategory+" "+type;
					return new ResponseEntity<>(msg.trim(),HttpStatus.OK);
				}else if(Constants.DEPARTMENT_SURCHARGE_MAP.containsKey(department)) {
					
					// to calculate fee with surcharge
					float surcharge = Constants.DEPARTMENT_SURCHARGE_MAP.get(department);
					int baseFeeWithSurCharge = Math.round(baseFee + (baseFee * surcharge));
					return new ResponseEntity<>(baseFeeWithSurCharge,HttpStatus.OK);
				}else {
					
					return new ResponseEntity<>("No surcharge found for department: "+department+
							" only base fee:"+ Math.round(baseFee),HttpStatus.OK);
				}
			}
		} catch (Exception e) {
			// Log the exception for debugging purposes
			logger.error("Error while calculating the fee: {}", e.getMessage(), e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * getBaseFee method calculates the base fee based on the filter values 
	 * base fee = qty * price
	 * @param department, category, sub_category, type
	 * @return base fee
	 */
	public Float getBaseFee(String department, String category, String subCategory, String type) {
		Float baseFee = 0f;
		List<FeeCsvEntity> feeDetails = repo.getDepartmentDetails(department,category,subCategory,type);

		//to calculate the fee
		for(FeeCsvEntity feeDetail : feeDetails){
			try {
				baseFee = baseFee + Float.valueOf(feeDetail.getQuantity())
				* Float.valueOf(feeDetail.getPrice());
			} catch (NumberFormatException ex) {
				// Handle invalid numeric values
				logger.error("Invalid numeric value: {}", ex.getMessage(), ex);
				throw new RuntimeException("Invalid numeric value: " + ex.getMessage(), ex);
			}
		}
		return baseFee;
	}

}