/*
Import the overlay function for booking appointments from loggedPatient.js

  Import the deleteDoctor API function to remove doctors (admin role) from docotrServices.js

  Import function to fetch patient details (used during booking) from patientServices.js

  Function to create and return a DOM element for a single doctor card
    Create the main container for the doctor card
    Retrieve the current user role from localStorage
    Create a div to hold doctor information
    Create and set the doctor’s name
    Create and set the doctor's specialization
    Create and set the doctor's email
    Create and list available appointment times
    Append all info elements to the doctor info container
    Create a container for card action buttons
    === ADMIN ROLE ACTIONS ===
      Create a delete button
      Add click handler for delete button
     Get the admin token from localStorage
        Call API to delete the doctor
        Show result and remove card if successful
      Add delete button to actions container
   
    === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
      Create a book now button
      Alert patient to log in before booking
      Add button to actions container
  
    === LOGGED-IN PATIENT ROLE ACTIONS === 
      Create a book now button
      Handle booking logic for logged-in patient   
        Redirect if token not available
        Fetch patient data with token
        Show booking overlay UI with doctor and patient info
      Add button to actions container
   
  Append doctor info and action buttons to the car
  Return the complete doctor card element
*/

// doctorCard.js
// Creates and returns a fully wired DOM element representing a single doctor card.
// Rendered content and available actions differ by the current user's role.

import {deleteDoctor} from '../services/doctorServices.js';
import {getPatientData} from '../services/patientServices.js';
import {showBookingOverlay} from '../loggedPatient.js';

/**
 * @param {Object} doctor - Doctor data object from the API.
 * @returns {HTMLElement} A ready-to-append doctor card element.
 */
export function createDoctorCard(doctor) {
    // ── Card container ────────────────────────────────────────────────────────
    const card = document.createElement("div");
    card.classList.add("doctor-card");

    const role = localStorage.getItem("userRole");

    // ── Doctor info section ───────────────────────────────────────────────────
    const infoDiv = document.createElement("div");
    infoDiv.classList.add("doctor-info");

    const name = document.createElement("h3");
    name.textContent = doctor.name;

    const specialization = document.createElement("p");
    specialization.textContent = `Specialty: ${doctor.specialty}`;

    const email = document.createElement("p");
    email.textContent = `Email: ${doctor.email}`;

    const availability = document.createElement("p");
    const times = Array.isArray(doctor.availableTimes)
        ? doctor.availableTimes.join(", ")
        : (doctor.availableTimes ?? "N/A");
    availability.textContent = `Available: ${times}`;

    infoDiv.appendChild(name);
    infoDiv.appendChild(specialization);
    infoDiv.appendChild(email);
    infoDiv.appendChild(availability);

    // ── Action buttons ────────────────────────────────────────────────────────
    const actionsDiv = document.createElement("div");
    actionsDiv.classList.add("card-actions");

    if (role === "admin") {
        // ── Admin: Delete doctor ────────────────────────────────────────────────
        const removeBtn = document.createElement("button");
        removeBtn.textContent = "Delete";

        removeBtn.addEventListener("click", async () => {
            if (!confirm(`Remove Dr. ${doctor.name}? This cannot be undone.`)) return;

            const token = localStorage.getItem("token");
            if (!token) {
                alert("❌ You must be logged in to delete a doctor.");
                return;
            }

            const {success, message} = await deleteDoctor(doctor.id, token);
            if (success) {
                alert(`✅ ${message}`);
                card.remove();
            } else {
                alert(`❌ ${message}`);
            }
        });

        actionsDiv.appendChild(removeBtn);

    } else if (role === "patient") {
        // ── Guest patient: prompt to log in ────────────────────────────────────
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";

        bookNow.addEventListener("click", () => {
            alert("Please log in or sign up to book an appointment.");
        });

        actionsDiv.appendChild(bookNow);

    } else if (role === "loggedPatient") {
        // ── Logged-in patient: open booking overlay ─────────────────────────────
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";

        bookNow.addEventListener("click", async (e) => {
            const token = localStorage.getItem("token");
            if (!token) {
                alert("Session expired. Please log in again.");
                window.location.href = "/";
                return;
            }

            const patientData = await getPatientData(token);
            if (!patientData) {
                alert("❌ Could not retrieve your patient data. Please try again.");
                return;
            }

            showBookingOverlay(e, doctor, patientData);
        });

        actionsDiv.appendChild(bookNow);
    }

    // ── Assemble and return ───────────────────────────────────────────────────
    card.appendChild(infoDiv);
    card.appendChild(actionsDiv);
    return card;
}
