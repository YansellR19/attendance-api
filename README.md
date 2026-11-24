# Control de Asistencia — API REST + Interfaz Web (Spring Boot 3)

Aplicacion completa en **Java 17 + Spring Boot 3** organizada en **Arquitectura en Capas** para el control de asistencia de empleados: registro de entrada y salida, turnos con tolerancia, calculo automatico de horas trabajadas y horas extra, consulta de historial, reportes diarios y estadisticas mensuales.

Incluye:
- **API REST v1** protegida con **JWT** (`ADMIN` / `EMPLOYEE`) y documentacion interactiva en **Swagger UI**.
- **Interfaz Grafica Web** integrada con **Thymeleaf** y control de acceso por roles.
- **PostgreSQL + Flyway** (migraciones SQL versionadas) + **perfil `dev`** con H2 en modo compatibilidad PostgreSQL para pruebas rapidas sin instalacion externa.

---

## Arquitectura en Capas

```text
com.attendance.api
├── config/             # SecurityConfig (dual JWT + Web), OpenApiConfig, ClockConfig, DataSeeder
├── security/           # JwtService, JwtAuthenticationFilter, CustomUserDetailsService, AuthenticatedUser
├── controller/
│   ├── api/            # @RestController -> /api/v1/** (JSON + JWT)
│   └── web/            # @Controller -> Vistas Thymeleaf (Sesion + CSRF)
├── service/            # AuthService, ShiftService, EmployeeService, AttendanceService, ReportService
├── repository/         # ShiftRepository, EmployeeRepository, AttendanceRecordRepository
├── entity/             # Shift, Employee, AttendanceRecord, Role, ArrivalStatus
├── dto/                # request/ y response/ (Java 17 records + Bean Validation)
├── mapper/             # AttendanceMapper (Entity <-> DTO)
└── exception/          # GlobalApiExceptionHandler (ProblemDetail RFC 7807) y GlobalWebExceptionHandler
```

---

## Requisitos
- **Java 17+** (incluye Maven Wrapper `./mvnw` / `.\mvnw.cmd`, no necesitas instalar Maven globalmente).
- **PostgreSQL 15/16** (opcional si ejecutas con el perfil `dev`).

---

## Como Ejecutar

### Opcion A: Con PostgreSQL (produccion / docker)
1. Levanta PostgreSQL (puedes usar `docker compose up -d` o tu propia instancia).
2. Configura las variables de entorno (ver `.env.example`):
   - `DB_URL` (por defecto `jdbc:postgresql://localhost:5432/attendance_db`)
   - `DB_USERNAME` (por defecto `attendance_user`)
   - `DB_PASSWORD` (por defecto `attendance_pass`)
   - `JWT_SECRET` (clave secreta >= 32 caracteres)
3. Ejecuta:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

### Opcion B: Modo Rapido Local (`dev` sin instalar PostgreSQL)
Usa H2 en modo compatibilidad PostgreSQL con las mismas migraciones Flyway y datos de demostracion:
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Una vez iniciado, abre en tu navegador:
- **Interfaz Web (Thymeleaf):** http://localhost:8080/login
- **Documentacion API (Swagger UI):** http://localhost:8080/swagger-ui.html

---

## Credenciales Iniciales de Demostracion
| Rol | Correo | Contrasena |
| :--- | :--- | :--- |
| `ADMIN` | `admin@asistencia.com` | `Admin123!` |
| `EMPLOYEE` | `ana.garcia@asistencia.com` | `Empleado123!` |
| `EMPLOYEE` | `carlos.lopez@asistencia.com` | `Empleado123!` |
| `EMPLOYEE` | `maria.perez@asistencia.com` | `Empleado123!` |

---

## Principales Endpoints REST (`/api/v1`)
- `POST /api/v1/auth/login` — Autenticacion y emision de JWT
- `GET /api/v1/auth/me` — Perfil del empleado autenticado
- `GET/POST/PUT /api/v1/shifts` — Gestion de turnos
- `GET/POST/PUT /api/v1/employees`, `PATCH /api/v1/employees/{id}/status` — Gestion de empleados (`ADMIN`)
- `POST /api/v1/attendances/check-in` — Marcar entrada
- `POST /api/v1/attendances/check-out` — Marcar salida (calcula horas trabajadas y extras)
- `GET /api/v1/attendances/today` — Estado de la jornada de hoy
- `GET /api/v1/attendances/me` — Historial personal paginado con filtros
- `GET /api/v1/attendances` — Historial global paginado (`ADMIN`)
- `PATCH /api/v1/attendances/{id}` — Ajuste manual con auditoria (`ADMIN`)
- `GET /api/v1/reports/daily?date=YYYY-MM-DD&department=...` — Reporte diario (`ADMIN`)
- `GET /api/v1/reports/monthly?year=2026&month=11&department=...` — Estadisticas mensuales globales (`ADMIN`)
- `GET /api/v1/reports/monthly/me?year=2026&month=11` — Estadisticas mensuales propias

---

## Pruebas Automatizadas
```powershell
.\mvnw.cmd test
```
