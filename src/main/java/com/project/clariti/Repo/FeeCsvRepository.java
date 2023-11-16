package com.project.clariti.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.clariti.Entity.FeeCsvEntity;

@Repository
public interface FeeCsvRepository extends JpaRepository<FeeCsvEntity,String>{
	
	@Query(value="SELECT * FROM FEE_CSV_TBL where department=?1 \n"
			+ "AND (category = ?2 OR ?2 = '') \n"
			+ "AND (sub_category = ?3 OR ?3 = '') \n"
			+ "AND (type = ?4 OR ?4 = '') ",nativeQuery = true)
	public List<FeeCsvEntity> getDepartmentDetails(String department, String category, String subCategory, String type);

}
