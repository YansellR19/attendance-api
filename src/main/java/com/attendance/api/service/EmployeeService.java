package com.attendance.api.service;

import com.attendance.api.dto.request.EmployeeCreateRequest;
import com.attendance.api.dto.request.EmployeeUpdateRequest;
import com.attendance.api.dto.response.EmployeeResponse;
import com.attendance.api.dto.response.PageResponse;
import com.attendance.api.entity.Employee;
import com.attendance.api.entity.Shift;
import com.attendance.api.exception.DuplicateResourceException;
import com.attendance.api.exception.ResourceNotFoundException;
import com.attendance.api.mapper.AttendanceMapper;
import com.attendance.api.repository.EmployeeRepository;
import com.attendance.api.repository.ShiftRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceMapper mapper;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            ShiftRepository shiftRepository,
            PasswordEncoder passwordEncoder,
            AttendanceMapper mapper
    ) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> findAll(String department, Boolean active, Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (department != null && !department.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase()));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<EmployeeResponse> page = employeeRepository.findAll(spec, pageable)
                .map(mapper::toEmployeeResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAllList() {
        return employeeRepository.findAllByOrderByFullNameAsc().stream()
                .map(mapper::toEmployeeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctDepartments() {
        return employeeRepository.findDistinctDepartments();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeRepository.findWithShiftById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));
        return mapper.toEmployeeResponse(employee);
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        String code = request.employeeCode().trim().toUpperCase();
        String email = request.email().trim().toLowerCase();

        if (employeeRepository.existsByEmployeeCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Ya existe un empleado con el codigo: " + code);
        }
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe un empleado con el correo: " + email);
        }

        Shift shift = shiftRepository.findById(request.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + request.shiftId()));

        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        employee.setRole(request.role());
        employee.setDepartment(request.department().trim());
        employee.setShift(shift);
        employee.setActive(true);

        return mapper.toEmployeeResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findWithShiftById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));

        String code = request.employeeCode().trim().toUpperCase();
        String email = request.email().trim().toLowerCase();

        if (employeeRepository.existsByEmployeeCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Ya existe otro empleado con el codigo: " + code);
        }
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Ya existe otro empleado con el correo: " + email);
        }

        Shift shift = shiftRepository.findById(request.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + request.shiftId()));

        employee.setEmployeeCode(code);
        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            employee.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        employee.setRole(request.role());
        employee.setDepartment(request.department().trim());
        employee.setShift(shift);
        if (request.active() != null) {
            employee.setActive(request.active());
        }

        return mapper.toEmployeeResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateStatus(Long id, boolean active) {
        Employee employee = employeeRepository.findWithShiftById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));
        employee.setActive(active);
        return mapper.toEmployeeResponse(employeeRepository.save(employee));
    }
}
