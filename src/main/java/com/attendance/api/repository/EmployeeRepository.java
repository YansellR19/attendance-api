package com.attendance.api.repository;

import com.attendance.api.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "shift")
    Optional<Employee> findWithShiftByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "shift")
    Optional<Employee> findWithShiftById(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String employeeCode, Long id);

    @EntityGraph(attributePaths = "shift")
    List<Employee> findByActiveTrueOrderByFullNameAsc();

    @EntityGraph(attributePaths = "shift")
    List<Employee> findByActiveTrueAndDepartmentIgnoreCaseOrderByFullNameAsc(String department);

    @EntityGraph(attributePaths = "shift")
    List<Employee> findAllByOrderByFullNameAsc();

    @Override
    @EntityGraph(attributePaths = "shift")
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    @Query("SELECT DISTINCT e.department FROM Employee e ORDER BY e.department ASC")
    List<String> findDistinctDepartments();
}
