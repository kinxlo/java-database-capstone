package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService
    ) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return List.of();
        }

        Doctor doctor = doctorOpt.get();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        List<String> available = new ArrayList<>(doctor.getAvailableTimes());
        for (Appointment appointment : appointments) {
            String bookedStart = appointment.getAppointmentTime().toLocalTime().toString().substring(0, 5);
            available.removeIf(slot -> slot.startsWith(bookedStart));
        }
        return available;
    }

    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getEmail() == null || doctor.getEmail().isBlank()) {
                return 0;
            }
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getId() == null) {
                return 0;
            }

            Optional<Doctor> existingOpt = doctorRepository.findById(doctor.getId());
            if (existingOpt.isEmpty()) {
                return -1;
            }

            Doctor existing = existingOpt.get();
            existing.setName(doctor.getName());
            existing.setSpecialty(doctor.getSpecialty());
            existing.setEmail(doctor.getEmail());
            existing.setPhone(doctor.getPhone());
            if (doctor.getPassword() != null && !doctor.getPassword().isBlank()) {
                existing.setPassword(doctor.getPassword());
            }
            if (doctor.getAvailableTimes() != null) {
                existing.setAvailableTimes(doctor.getAvailableTimes());
            }

            doctorRepository.save(existing);
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    @Transactional
    public int deleteDoctor(long id) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(id);
            if (doctorOpt.isEmpty()) {
                return -1;
            }

            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();

        if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
            response.put("message", "Invalid credentials.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
        if (doctor == null) {
            response.put("message", "Doctor not found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!doctor.getPassword().equals(login.getPassword())) {
            response.put("message", "Invalid credentials.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        response.put("token", generateTokenForDoctor(doctor.getEmail()));
        response.put("message", "Login successful.");
        return ResponseEntity.ok(response);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameLike(name == null ? "" : name);
        result.put("doctors", doctors);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                name == null ? "" : name,
                specialty == null ? "" : specialty
        );
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameLike(name == null ? "" : name);
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                name == null ? "" : name,
                specilty == null ? "" : specilty
        );
        result.put("doctors", doctors);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty);
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty);
        result.put("doctors", doctors);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findAll();
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) {
            return doctors;
        }

        boolean isAm = "AM".equalsIgnoreCase(amOrPm);
        List<Doctor> filtered = new ArrayList<>();

        for (Doctor doctor : doctors) {
            boolean matches = doctor.getAvailableTimes().stream().anyMatch(slot -> {
                String start = slot.split("-")[0].trim();
                LocalTime time = LocalTime.parse(start);
                return isAm ? time.getHour() < 12 : time.getHour() >= 12;
            });

            if (matches) {
                filtered.add(doctor);
            }
        }

        return filtered;
    }

    private String generateTokenForDoctor(String identifier) {
        try {
            Method method = tokenService.getClass().getMethod("generateToken", String.class);
            Object value = method.invoke(tokenService, identifier);
            return value instanceof String s ? s : identifier;
        } catch (Exception ignore) {
            return identifier;
        }
    }
}
