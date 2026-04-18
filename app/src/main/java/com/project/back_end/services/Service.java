package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class Service {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(
            TokenService tokenService,
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorService doctorService,
            PatientService patientService
    ) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> response = new HashMap<>();

        boolean isValid = tokenService.validateToken(token, user);
        if (!isValid) {
            response.put("message", "Invalid or expired token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        response.put("message", "Token is valid.");
        return ResponseEntity.ok(response);
    }

    /**
     * Compatibility helper for MVC routes that only need error-map semantics.
     */
    public Map<String, String> validateTokenMap(String token, String user) {
        ResponseEntity<Map<String, String>> validation = validateToken(token, user);
        return validation.getStatusCode().is2xxSuccessful() ? Map.of() : validation.getBody();
    }

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();

        try {
            if (receivedAdmin == null || receivedAdmin.getUsername() == null || receivedAdmin.getPassword() == null) {
                response.put("message", "Invalid credentials.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || !admin.getPassword().equals(receivedAdmin.getPassword())) {
                response.put("message", "Invalid username or password.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            response.put("token", tokenService.generateToken(admin.getUsername()));
            response.put("message", "Login successful.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("message", "Failed to validate admin login.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = name != null && !name.isBlank() && !"null".equalsIgnoreCase(name);
        boolean hasSpecialty = specialty != null && !specialty.isBlank() && !"null".equalsIgnoreCase(specialty);
        boolean hasTime = time != null && !time.isBlank() && !"null".equalsIgnoreCase(time);

        if (hasName && hasSpecialty && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        }
        if (hasName && hasSpecialty) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        }
        if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        }
        if (hasSpecialty && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        }
        if (hasName) {
            return doctorService.findDoctorByName(name);
        }
        if (hasSpecialty) {
            return doctorService.filterDoctorBySpecility(specialty);
        }
        if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctorService.getDoctors());
        return response;
    }

    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                || appointment.getAppointmentTime() == null) {
            return 0;
        }

        Long doctorId = appointment.getDoctor().getId();
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return -1;
        }

        LocalDate date = appointment.getAppointmentTime().toLocalDate();
        List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);

        String requestedStart = appointment.getAppointmentTime().toLocalTime().toString();
        if (requestedStart.length() >= 5) {
            requestedStart = requestedStart.substring(0, 5);
        }
        final String requestedStartFinal = requestedStart;

        boolean valid = availableSlots.stream().anyMatch(slot -> {
            String start = slot.split("-")[0].trim();
            if (start.length() >= 5) {
                start = start.substring(0, 5);
            }
            return start.equals(requestedStartFinal);
        });

        return valid ? 1 : 0;
    }

    public boolean validatePatient(Patient patient) {
        if (patient == null) {
            return false;
        }
        Patient existing = patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone());
        return existing == null;
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();

        try {
            if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
                response.put("message", "Invalid credentials.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Patient patient = patientRepository.findByEmail(login.getIdentifier());
            if (patient == null || !patient.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            response.put("token", tokenService.generateToken(patient.getEmail()));
            response.put("message", "Login successful.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("message", "Failed to validate patient login.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        String email = tokenService.extractEmail(token);
        Patient patient = patientRepository.findByEmail(email);

        if (patient == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient not found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        boolean hasCondition = condition != null && !condition.isBlank() && !"null".equalsIgnoreCase(condition);
        boolean hasName = name != null && !name.isBlank() && !"null".equalsIgnoreCase(name);

        if (hasCondition && hasName) {
            return patientService.filterByDoctorAndCondition(condition, name, patient.getId());
        }
        if (hasCondition) {
            return patientService.filterByCondition(condition, patient.getId());
        }
        if (hasName) {
            return patientService.filterByDoctor(name, patient.getId());
        }

        return patientService.getPatientAppointment(patient.getId(), token);
    }
}
