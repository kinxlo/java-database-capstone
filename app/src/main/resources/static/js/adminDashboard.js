/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/

// adminDashboard.js
// Admin dashboard logic: load, filter, and add doctors.

import {openModal} from './components/modals.js';
import {getDoctors, filterDoctors, saveDoctor} from './services/doctorServices.js';
import {createDoctorCard} from './components/doctorCard.js';

// ── "Add Doctor" button (injected by header.js after DOM is ready) ────────────
document.addEventListener("DOMContentLoaded", () => {
    loadDoctorCards();

    // The addDocBtn is rendered inside the header by header.js; attach after load
    const addDocBtn = document.getElementById("addDocBtn");
    if (addDocBtn) {
        addDocBtn.addEventListener("click", () => openModal("addDoctor"));
    }

    // Search + filter listeners
    document.getElementById("searchBar")
        .addEventListener("input", filterDoctorsOnChange);
    document.getElementById("filterTime")
        .addEventListener("change", filterDoctorsOnChange);
    document.getElementById("filterSpecialty")
        .addEventListener("change", filterDoctorsOnChange);
});

// ── Load all doctor cards ─────────────────────────────────────────────────────
async function loadDoctorCards() {
    try {
        const doctors = await getDoctors();
        const contentDiv = document.getElementById("content");
        contentDiv.innerHTML = "";
        doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
    } catch (error) {
        console.error("Error :: loadDoctorCards ::", error);
    }
}

// ── Filter doctors on search/dropdown change ──────────────────────────────────
async function filterDoctorsOnChange() {
    const nameVal = document.getElementById("searchBar").value.trim();
    const timeVal = document.getElementById("filterTime").value;
    const specialtyVal = document.getElementById("filterSpecialty").value;

    const name = nameVal || null;
    const time = timeVal || null;
    const specialty = specialtyVal || null;

    try {
        const response = await filterDoctors(name, time, specialty);
        const doctors = response.doctors ?? [];

        if (doctors.length > 0) {
            renderDoctorCards(doctors);
        } else {
            document.getElementById("content").innerHTML =
                "<p>No doctors found with the given filters.</p>";
        }
    } catch (error) {
        console.error("Error :: filterDoctorsOnChange ::", error);
        alert("❌ An error occurred while filtering doctors.");
    }
}

// ── Utility: render a list of doctor cards ────────────────────────────────────
function renderDoctorCards(doctors) {
    const contentDiv = document.getElementById("content");
    contentDiv.innerHTML = "";
    doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
}

// ── Collect modal form data and save new doctor (called from modal button) ────
window.adminAddDoctor = async function adminAddDoctor() {
    const name = document.getElementById("doctorName").value.trim();
    const specialty = document.getElementById("specialization").value;
    const email = document.getElementById("doctorEmail").value.trim();
    const password = document.getElementById("doctorPassword").value;
    const phone = document.getElementById("doctorPhone").value.trim();

    // Collect checked availability time slots
    const checkedBoxes = document.querySelectorAll('input[name="availability"]:checked');
    const availableTimes = Array.from(checkedBoxes).map(cb => cb.value);

    const token = localStorage.getItem("token");
    if (!token) {
        alert("❌ You must be logged in as Admin to add a doctor.");
        return;
    }

    const doctor = {name, specialty, email, password, phone, availableTimes};

    const {success, message} = await saveDoctor(doctor, token);
    if (success) {
        alert(`✅ ${message}`);
        document.getElementById("modal").style.display = "none";
        await loadDoctorCards();
    } else {
        alert(`❌ ${message}`);
    }
};
