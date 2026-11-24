package com.attendance.api.service;

import com.attendance.api.dto.request.ShiftRequest;
import com.attendance.api.dto.response.ShiftResponse;
import com.attendance.api.entity.Shift;
import com.attendance.api.exception.BusinessRuleException;
import com.attendance.api.exception.DuplicateResourceException;
import com.attendance.api.exception.ResourceNotFoundException;
import com.attendance.api.mapper.AttendanceMapper;
import com.attendance.api.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final AttendanceMapper mapper;

    public ShiftService(ShiftRepository shiftRepository, AttendanceMapper mapper) {
        this.shiftRepository = shiftRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> findAll() {
        return shiftRepository.findAllByOrderByIdAsc().stream()
                .map(mapper::toShiftResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> findActive() {
        return shiftRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toShiftResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftResponse findById(Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + id));
        return mapper.toShiftResponse(shift);
    }

    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        validateShiftTimes(request);
        String trimmedName = request.name().trim();
        if (shiftRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Ya existe un turno con el nombre: " + trimmedName);
        }

        Shift shift = new Shift();
        applyRequest(shift, request);
        shift.setActive(request.active() == null || request.active());
        return mapper.toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        validateShiftTimes(request);
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + id));

        String trimmedName = request.name().trim();
        if (shiftRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Ya existe otro turno con el nombre: " + trimmedName);
        }

        applyRequest(shift, request);
        if (request.active() != null) {
            shift.setActive(request.active());
        }
        return mapper.toShiftResponse(shiftRepository.save(shift));
    }

    private void validateShiftTimes(ShiftRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessRuleException("La hora de fin del turno debe ser posterior a la hora de inicio.");
        }
    }

    private void applyRequest(Shift shift, ShiftRequest request) {
        shift.setName(request.name().trim());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setGraceMinutes(request.graceMinutes());
        shift.setBreakMinutes(request.breakMinutes());
        shift.setExpectedWorkMinutes(request.expectedWorkMinutes());
        shift.setWorkDaySet(request.workDays());
    }
}
