package com.attendance.api.service;

import com.attendance.api.dto.request.AttendanceAdjustRequest;
import com.attendance.api.dto.response.AttendanceResponse;
import com.attendance.api.dto.response.PageResponse;
import com.attendance.api.entity.ArrivalStatus;
import com.attendance.api.entity.AttendanceRecord;
import com.attendance.api.entity.Employee;
import com.attendance.api.entity.Shift;
import com.attendance.api.exception.BusinessRuleException;
import com.attendance.api.exception.ResourceNotFoundException;
import com.attendance.api.mapper.AttendanceMapper;
import com.attendance.api.repository.AttendanceRecordRepository;
import com.attendance.api.repository.EmployeeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper mapper;
    private final Clock clock;
    private final ZoneId zoneId;

    public AttendanceService(
            AttendanceRecordRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            AttendanceMapper mapper,
            Clock clock,
            ZoneId zoneId
    ) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.mapper = mapper;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    @Transactional
    public AttendanceResponse checkIn(Long employeeId, String notes) {
        Employee employee = employeeRepository.findWithShiftById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + employeeId));

        if (!employee.isActive()) {
            throw new BusinessRuleException("El empleado esta inactivo y no puede registrar asistencia.");
        }

        Instant now = clock.instant();
        ZonedDateTime localNow = now.atZone(zoneId);
        LocalDate workDate = localNow.toLocalDate();

        if (attendanceRepository.existsByEmployeeIdAndWorkDate(employeeId, workDate)) {
            throw new BusinessRuleException("Ya registraste tu entrada para el dia de hoy (" + workDate + ").");
        }

        Shift shift = employee.getShift();
        ArrivalEvaluation arrival = evaluateArrival(shift, localNow.toLocalTime());

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setShift(shift);
        record.setWorkDate(workDate);
        record.setCheckInAt(now);
        record.setArrivalStatus(arrival.status());
        record.setLateMinutes(arrival.lateMinutes());
        record.setOvertimeMinutes(0);
        if (notes != null && !notes.isBlank()) {
            record.setNotes(notes.trim());
        }

        return mapper.toAttendanceResponse(attendanceRepository.save(record), zoneId);
    }

    @Transactional
    public AttendanceResponse checkOut(Long employeeId, String notes) {
        Instant now = clock.instant();
        LocalDate workDate = now.atZone(zoneId).toLocalDate();

        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, workDate)
                .orElseThrow(() -> new BusinessRuleException("No tienes una entrada registrada hoy para marcar salida."));

        if (!record.isOpen()) {
            throw new BusinessRuleException("Ya registraste tu salida para el dia de hoy (" + workDate + ").");
        }

        if (now.isBefore(record.getCheckInAt())) {
            throw new BusinessRuleException("La hora de salida no puede ser anterior a la hora de entrada.");
        }

        WorkDuration duration = calculateWorkDuration(record.getCheckInAt(), now, record.getShift());
        record.setCheckOutAt(now);
        record.setWorkedMinutes(duration.workedMinutes());
        record.setOvertimeMinutes(duration.overtimeMinutes());

        if (notes != null && !notes.isBlank()) {
            String trimmed = notes.trim();
            if (record.getNotes() == null || record.getNotes().isBlank()) {
                record.setNotes(trimmed);
            } else {
                record.setNotes(record.getNotes() + " | Salida: " + trimmed);
            }
        }

        return mapper.toAttendanceResponse(attendanceRepository.save(record), zoneId);
    }

    @Transactional
    public AttendanceResponse adjustAttendance(Long recordId, Long adminEmployeeId, AttendanceAdjustRequest request) {
        AttendanceRecord record = attendanceRepository.findWithDetailsById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de asistencia no encontrado con id: " + recordId));

        Employee admin = employeeRepository.findById(adminEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con id: " + adminEmployeeId));

        Instant newCheckIn = request.checkInTime() != null
                ? record.getWorkDate().atTime(request.checkInTime()).atZone(zoneId).toInstant()
                : record.getCheckInAt();

        Instant newCheckOut = request.checkOutTime() != null
                ? record.getWorkDate().atTime(request.checkOutTime()).atZone(zoneId).toInstant()
                : record.getCheckOutAt();

        if (newCheckOut != null && newCheckOut.isBefore(newCheckIn)) {
            throw new BusinessRuleException("La hora de salida ajustada no puede ser anterior a la entrada.");
        }

        LocalTime localCheckIn = newCheckIn.atZone(zoneId).toLocalTime();
        ArrivalEvaluation arrival = evaluateArrival(record.getShift(), localCheckIn);
        record.setCheckInAt(newCheckIn);
        record.setArrivalStatus(arrival.status());
        record.setLateMinutes(arrival.lateMinutes());

        if (newCheckOut != null) {
            WorkDuration duration = calculateWorkDuration(newCheckIn, newCheckOut, record.getShift());
            record.setCheckOutAt(newCheckOut);
            record.setWorkedMinutes(duration.workedMinutes());
            record.setOvertimeMinutes(duration.overtimeMinutes());
        }

        record.setAdjustedBy(admin);
        record.setNotes("[Ajuste manual] " + request.notes().trim());

        return mapper.toAttendanceResponse(attendanceRepository.save(record), zoneId);
    }

    @Transactional(readOnly = true)
    public Optional<AttendanceResponse> getTodayRecord(Long employeeId) {
        LocalDate today = LocalDate.now(clock);
        return attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .map(r -> mapper.toAttendanceResponse(r, zoneId));
    }

    @Transactional(readOnly = true)
    public AttendanceResponse findById(Long id) {
        AttendanceRecord record = attendanceRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de asistencia no encontrado con id: " + id));
        return mapper.toAttendanceResponse(record, zoneId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getMyHistory(
            Long employeeId,
            LocalDate from,
            LocalDate to,
            ArrivalStatus status,
            Pageable pageable
    ) {
        return searchHistory(employeeId, null, from, to, status, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> searchHistory(
            Long employeeId,
            String department,
            LocalDate from,
            LocalDate to,
            ArrivalStatus status,
            Boolean openOnly,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        Specification<AttendanceRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("employee").get("department")), department.trim().toLowerCase()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("workDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("workDate"), to));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("arrivalStatus"), status));
            }
            if (Boolean.TRUE.equals(openOnly)) {
                predicates.add(cb.isNull(root.get("checkOutAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AttendanceResponse> page = attendanceRepository.findAll(spec, pageable)
                .map(r -> mapper.toAttendanceResponse(r, zoneId));
        return PageResponse.from(page);
    }

    public ArrivalEvaluation evaluateArrival(Shift shift, LocalTime checkInLocalTime) {
        LocalTime graceDeadline = shift.getStartTime().plusMinutes(shift.getGraceMinutes());
        if (checkInLocalTime.isAfter(graceDeadline)) {
            long minsLate = Duration.between(shift.getStartTime(), checkInLocalTime).toMinutes();
            return new ArrivalEvaluation(ArrivalStatus.LATE, (int) Math.max(1, minsLate));
        }
        return new ArrivalEvaluation(ArrivalStatus.ON_TIME, 0);
    }

    public WorkDuration calculateWorkDuration(Instant checkInAt, Instant checkOutAt, Shift shift) {
        int grossMinutes = (int) Math.max(0, Duration.between(checkInAt, checkOutAt).toMinutes());
        int breakThreshold = (shift.getExpectedWorkMinutes() / 2) + shift.getBreakMinutes();
        int workedMinutes = (shift.getBreakMinutes() > 0 && grossMinutes > breakThreshold)
                ? Math.max(0, grossMinutes - shift.getBreakMinutes())
                : grossMinutes;
        int overtimeMinutes = Math.max(0, workedMinutes - shift.getExpectedWorkMinutes());
        return new WorkDuration(workedMinutes, overtimeMinutes);
    }

    public record ArrivalEvaluation(ArrivalStatus status, int lateMinutes) {
    }

    public record WorkDuration(int workedMinutes, int overtimeMinutes) {
    }
}
