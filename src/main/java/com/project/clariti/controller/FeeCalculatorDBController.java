package com.project.clariti.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.clariti.service.FeeCalculatorDBService;

@RestController
@RequestMapping("/")
public class FeeCalculatorDBController {
	
	@Autowired
	public FeeCalculatorDBService service;
	
	@GetMapping(value = "calculateFeeFromDB")
	public ResponseEntity<Object> getDepartmentFee(
			@RequestParam(name="department") String department,
			@RequestParam(name="category",required = false, defaultValue = "") String category,
			@RequestParam(name="subCategory",required = false, defaultValue = "") String subCategory,
			@RequestParam(name="type",required = false, defaultValue = "") String type){
		return service.getDepartmentFeeFromDb(department,category,subCategory,type);
	}
	
	@PostMapping("/uploadCsv")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            service.loadCsvDataIntoDatabase(file);
            return ResponseEntity.status(HttpStatus.OK).body("CSV file uploaded and data processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing CSV file: " + e.getMessage());
        }
    }

}