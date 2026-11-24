INSERT INTO shifts (name, start_time, end_time, grace_minutes, break_minutes, expected_work_minutes, work_days, active)
VALUES
    ('Matutino Estandar (09:00 - 18:00)', '09:00:00', '18:00:00', 10, 60, 480, 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', TRUE),
    ('Temprano (08:00 - 17:00)', '08:00:00', '17:00:00', 10, 60, 480, 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', TRUE),
    ('Medio Tiempo (09:00 - 13:00)', '09:00:00', '13:00:00', 5, 0, 240, 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY', TRUE);
