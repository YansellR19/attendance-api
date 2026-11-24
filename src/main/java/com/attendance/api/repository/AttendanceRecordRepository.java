package com.attendance.api.repository;

import com.attendance.api.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {

    @EntityGraph(attributePaths = {"employee", "shift", "adjustedBy"})
    Optional<AttendanceRecord> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"employee", "shift", "adjustedBy"})
    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    @EntityGraph(attributePaths = {"employee", "shift", "adjustedBy"})
    List<AttendanceRecord> findByWorkDateOrderByCheckInAtAsc(LocalDate workDate);

    @EntityGraph(attributePaths = {"employee", "shift", "adjustedBy"})
    List<AttendanceRecord> findByWorkDateBetweenOrderByWorkDateAsc(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"employee", "shift", "adjustedBy"})
    List<AttendanceRecord> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(Long employeeId, LocalDate from, LocalDate to);

    boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    @Override
    @EntityGraph(attributePaths = {"employee", "shift", "adjustedBy"})
    Page<AttendanceRecord> findAll(Specification<AttendanceRecord> spec, Pageable pageable);
}
