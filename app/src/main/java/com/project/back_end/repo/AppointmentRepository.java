package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT DISTINCT a
            FROM Appointment a
            LEFT JOIN FETCH a.doctor d
            LEFT JOIN FETCH d.availableTimes
            WHERE d.id = :doctorId
              AND a.appointmentTime BETWEEN :start AND :end
            """)
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT DISTINCT a
            FROM Appointment a
            LEFT JOIN FETCH a.doctor d
            LEFT JOIN FETCH a.patient p
            LEFT JOIN FETCH d.availableTimes
            WHERE d.id = :doctorId
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%'))
              AND a.appointmentTime BETWEEN :start AND :end
            """)
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId")
    List<Appointment> findByPatientId(@Param("patientId") Long patientId);

    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.patient.id = :patientId
              AND LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
            ORDER BY a.appointmentTime ASC
            """)
    List<Appointment> filterByDoctorNameAndPatientId(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId
    );

    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.patient.id = :patientId
              AND a.status = :status
              AND LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
            ORDER BY a.appointmentTime ASC
            """)
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId,
            @Param("status") int status
    );
}
