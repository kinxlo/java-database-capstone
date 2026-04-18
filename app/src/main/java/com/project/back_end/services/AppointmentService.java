package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            TokenService tokenService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            if (appointment == null
                    || appointment.getDoctor() == null
                    || appointment.getDoctor().getId() == null
                    || appointment.getPatient() == null
                    || appointment.getPatient().getId() == null
                    || appointment.getAppointmentTime() == null) {
                return 0;
            }

            Optional<Doctor> doctorOpt = doctorRepository.findById(appointment.getDoctor().getId());
            Optional<Patient> patientOpt = patientRepository.findById(appointment.getPatient().getId());
            if (doctorOpt.isEmpty() || patientOpt.isEmpty()) {
                return 0;
            }

            if (isSlotTaken(appointment.getDoctor().getId(), appointment.getAppointmentTime(), null)) {
                return 0;
            }

            appointment.setDoctor(doctorOpt.get());
            appointment.setPatient(patientOpt.get());
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();

        try {
            if (appointment == null || appointment.getId() == null) {
                response.put("message", "Appointment id is required.");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Appointment> existingOpt = appointmentRepository.findById(appointment.getId());
            if (existingOpt.isEmpty()) {
                response.put("message", "Appointment not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment existing = existingOpt.get();

            if (appointment.getPatient() == null || appointment.getPatient().getId() == null) {
                response.put("message", "Patient id is required.");
                return ResponseEntity.badRequest().body(response);
            }

            if (!existing.getPatient().getId().equals(appointment.getPatient().getId())) {
                response.put("message", "You cannot update another patient's appointment.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null || appointment.getAppointmentTime() == null) {
                response.put("message", "Doctor and appointment time are required.");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Doctor> doctorOpt = doctorRepository.findById(appointment.getDoctor().getId());
            if (doctorOpt.isEmpty()) {
                response.put("message", "Invalid doctor id.");
                return ResponseEntity.badRequest().body(response);
            }

            if (isSlotTaken(appointment.getDoctor().getId(), appointment.getAppointmentTime(), appointment.getId())) {
                response.put("message", "Selected slot is already booked.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            existing.setDoctor(doctorOpt.get());
            existing.setAppointmentTime(appointment.getAppointmentTime());
            existing.setStatus(appointment.getStatus());
            appointmentRepository.save(existing);

            response.put("message", "Appointment updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("message", "Failed to update appointment.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
            if (appointmentOpt.isEmpty()) {
                response.put("message", "Appointment not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (token == null || token.isBlank()) {
                response.put("message", "Missing token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Appointment appointment = appointmentOpt.get();
            String email = extractEmailFromToken(token);
            Patient patient = patientRepository.findByEmail(email);

            if (patient == null || !appointment.getPatient().getId().equals(patient.getId())) {
                response.put("message", "You are not authorized to cancel this appointment.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            appointmentRepository.delete(appointment);
            response.put("message", "Appointment canceled successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("message", "Failed to cancel appointment.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (token == null || token.isBlank()) {
                result.put("message", "Missing token.");
                result.put("appointments", List.of());
                return result;
            }

            String email = extractEmailFromToken(token);
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null) {
                result.put("message", "Doctor not found for token.");
                result.put("appointments", List.of());
                return result;
            }

            LocalDate selectedDate = date != null ? date : LocalDate.now();
            LocalDateTime start = selectedDate.atStartOfDay();
            LocalDateTime end = selectedDate.plusDays(1).atStartOfDay().minusNanos(1);

            List<Appointment> appointments;
            if (pname == null || pname.isBlank() || "null".equalsIgnoreCase(pname)) {
                appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
            } else {
                appointments = appointmentRepository
                        .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                doctor.getId(), pname, start, end
                        );
            }

            result.put("appointments", appointments);
            return result;
        } catch (Exception ex) {
            result.put("message", "Failed to retrieve appointments.");
            result.put("appointments", List.of());
            return result;
        }
    }

    @Transactional
    public void changeStatus(long appointmentId, int status) {
        appointmentRepository.findById(appointmentId).ifPresent(appointment -> {
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
        });
    }

    private boolean isSlotTaken(Long doctorId, LocalDateTime appointmentTime, Long currentAppointmentId) {
        LocalDateTime dayStart = appointmentTime.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = appointmentTime.toLocalDate().plusDays(1).atStartOfDay().minusNanos(1);

        List<Appointment> doctorAppointments =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, dayStart, dayEnd);

        return doctorAppointments.stream().anyMatch(existing -> {
            boolean sameTime = appointmentTime.equals(existing.getAppointmentTime());
            boolean sameRecord = currentAppointmentId != null && currentAppointmentId.equals(existing.getId());
            return sameTime && !sameRecord;
        });
    }

    /**
     * Uses TokenService.extractEmail(String) when available.
     * Falls back to raw token value to keep current scaffolding compatible.
     */
    private String extractEmailFromToken(String token) {
        try {
            Method method = tokenService.getClass().getMethod("extractEmail", String.class);
            Object value = method.invoke(tokenService, token);
            return value instanceof String s ? s : token;
        } catch (Exception ignore) {
            return token;
        }
    }
}
