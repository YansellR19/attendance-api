package com.attendance.api.service;

import com.attendance.api.entity.ArrivalStatus;
import com.attendance.api.entity.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceServiceUnitTest {

    private static final ZoneId ZONE = ZoneId.of("America/Santo_Domingo");
    private AttendanceService attendanceService;
    private Shift standardShift;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(null, null, null, Clock.system(ZONE), ZONE);
        standardShift = new Shift();
        standardShift.setName("Matutino 09:00-18:00");
        standardShift.setStartTime(LocalTime.of(9, 0));
        standardShift.setEndTime(LocalTime.of(18, 0));
        standardShift.setGraceMinutes(10);
        standardShift.setBreakMinutes(60);
        standardShift.setExpectedWorkMinutes(480);
    }

    @Test
    void evaluateArrival_withinGracePeriod_isOnTime() {
        AttendanceService.ArrivalEvaluation eval =
                attendanceService.evaluateArrival(standardShift, LocalTime.of(9, 10));

        assertThat(eval.status()).isEqualTo(ArrivalStatus.ON_TIME);
        assertThat(eval.lateMinutes()).isZero();
    }

    @Test
    void evaluateArrival_afterGracePeriod_isLateWithExactMinutes() {
        AttendanceService.ArrivalEvaluation eval =
                attendanceService.evaluateArrival(standardShift, LocalTime.of(9, 25));

        assertThat(eval.status()).isEqualTo(ArrivalStatus.LATE);
        assertThat(eval.lateMinutes()).isEqualTo(25);
    }

    @Test
    void calculateWorkDuration_fullDayWithOvertime_deductsBreakAndComputesOvertime() {
        LocalDate date = LocalDate.of(2026, 11, 24);
        Instant checkIn = date.atTime(9, 0).atZone(ZONE).toInstant();
        Instant checkOut = date.atTime(19, 30).atZone(ZONE).toInstant(); // 630m bruto -> -60m descanso = 570m -> 90m extra

        AttendanceService.WorkDuration duration =
                attendanceService.calculateWorkDuration(checkIn, checkOut, standardShift);

        assertThat(duration.workedMinutes()).isEqualTo(570);
        assertThat(duration.overtimeMinutes()).isEqualTo(90);
    }

    @Test
    void calculateWorkDuration_shortSession_doesNotSubtractBreak() {
        LocalDate date = LocalDate.of(2026, 11, 24);
        Instant checkIn = date.atTime(9, 0).atZone(ZONE).toInstant();
        Instant checkOut = date.atTime(10, 45).atZone(ZONE).toInstant();

        AttendanceService.WorkDuration duration =
                attendanceService.calculateWorkDuration(checkIn, checkOut, standardShift);

        assertThat(duration.workedMinutes()).isEqualTo(105);
        assertThat(duration.overtimeMinutes()).isZero();
    }
}
