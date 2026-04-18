// loggedPatient.js
// Dashboard + booking logic for logged-in patients.

import {getDoctors, filterDoctors} from './services/doctorServices.js';
import {createDoctorCard} from './components/doctorCard.js';
import {bookAppointment} from './services/appointmentRecordService.js';

// ── Load all doctor cards on page load ────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    loadDoctorCards();
});

function loadDoctorCards() {
    getDoctors()
        .then(doctors => {
            const contentDiv = document.getElementById("content");
            contentDiv.innerHTML = "";
            doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
        })
        .catch(error => console.error("Failed to load doctors:", error));
}

// ── Booking overlay (exported for use in doctorCard.js) ───────────────────────
export function showBookingOverlay(e, doctor, patient) {
    const ripple = document.createElement("div");
    ripple.classList.add("ripple-overlay");
    ripple.style.left = `${e.clientX}px`;
    ripple.style.top = `${e.clientY}px`;
    document.body.appendChild(ripple);
    setTimeout(() => ripple.classList.add("active"), 50);

    const modalApp = document.createElement("div");
    modalApp.classList.add("modalApp");
    modalApp.innerHTML = `
    <h2>Book Appointment</h2>
    <input class="input-field" type="text"  value="${patient.name}"    disabled />
    <input class="input-field" type="text"  value="${doctor.name}"     disabled />
    <input class="input-field" type="text"  value="${doctor.specialty}" disabled />
    <input class="input-field" type="email" value="${doctor.email}"    disabled />
    <input class="input-field" type="date"  id="appointment-date" />
    <select class="input-field" id="appointment-time">
      <option value="">Select time</option>
      ${(doctor.availableTimes ?? []).map(t => `<option value="${t}">${t}</option>`).join('')}
    </select>
    <button class="confirm-booking">Confirm Booking</button>
  `;

    document.body.appendChild(modalApp);
    setTimeout(() => modalApp.classList.add("active"), 300);

    modalApp.querySelector(".confirm-booking").addEventListener("click", async () => {
        const date = modalApp.querySelector("#appointment-date").value;
        const time = modalApp.querySelector("#appointment-time").value;
        const token = localStorage.getItem("token");
        const startTime = time.split("-")[0];

        if (!date || !time) {
            alert("Please select both a date and a time slot.");
            return;
        }

        const appointment = {
            doctor: {id: doctor.id},
            patient: {id: patient.id},
            appointmentTime: `${date}T${startTime}:00`,
            status: 0,
        };

        const {success, message} = await bookAppointment(appointment, token);
        if (success) {
            alert("✅ Appointment booked successfully!");
            ripple.remove();
            modalApp.remove();
        } else {
            alert("❌ Failed to book appointment: " + message);
        }
    });
}

// ── Search + filter ───────────────────────────────────────────────────────────
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

function filterDoctorsOnChange() {
    const nameVal = document.getElementById("searchBar").value.trim();
    const timeVal = document.getElementById("filterTime").value;
    const specialtyVal = document.getElementById("filterSpecialty").value;

    const name = nameVal.length > 0 ? nameVal : null;
    const time = timeVal.length > 0 ? timeVal : null;
    const specialty = specialtyVal.length > 0 ? specialtyVal : null;

    filterDoctors(name, time, specialty)
        .then(response => {
            const doctors = response.doctors ?? [];
            const contentDiv = document.getElementById("content");
            contentDiv.innerHTML = "";
            if (doctors.length > 0) {
                doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
            } else {
                contentDiv.innerHTML = "<p>No doctors found with the given filters.</p>";
            }
        })
        .catch(error => {
            console.error("Failed to filter doctors:", error);
            alert("❌ An error occurred while filtering doctors.");
        });
}

// ── Utility export ────────────────────────────────────────────────────────────
export function renderDoctorCards(doctors) {
    const contentDiv = document.getElementById("content");
    contentDiv.innerHTML = "";
    doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
}
