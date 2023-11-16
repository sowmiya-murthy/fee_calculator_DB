package com.project.clariti.utils;

import java.util.HashMap;
import java.util.Map;

public class Constants {

	public static final int ID_INDEX = 0;
	public static final int NAME_INDEX = 1;
	public static final int DESC_INDEX = 2;
	public static final int DEPARTMENT_INDEX = 3;
	public static final int CATEGORY_INDEX = 4;
	public static final int SUBCATEGORY_INDEX = 5;
	public static final int TYPE_INDEX = 6;
	public static final int QUANTITY_INDEX = 7;
	public static final int PRICE_INDEX = 8;

	public static final String MARKETING_DEPARTMENT = "Marketing";
	public static final String SALES_DEPARTMENT = "Sales";
	public static final String DEVELOPMENT_DEPARTMENT = "Development";
	public static final String OPERATIONS_DEPARTMENT = "Operations";
	public static final String SUPPORT_DEPARTMENT = "Support";
	
	public static final Map<String, Float> DEPARTMENT_SURCHARGE_MAP = new HashMap<>();
	
	static {
		DEPARTMENT_SURCHARGE_MAP.put(MARKETING_DEPARTMENT, 0.1f);
		DEPARTMENT_SURCHARGE_MAP.put(SALES_DEPARTMENT, 0.15f);
		DEPARTMENT_SURCHARGE_MAP.put(DEVELOPMENT_DEPARTMENT, 0.2f);
		DEPARTMENT_SURCHARGE_MAP.put(OPERATIONS_DEPARTMENT, -0.15f);
		DEPARTMENT_SURCHARGE_MAP.put(SUPPORT_DEPARTMENT, -0.5f);
	}
}
