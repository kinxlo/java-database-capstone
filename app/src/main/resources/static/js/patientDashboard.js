// patientDashboard.js
import {getDoctors, filterDoctors} from './services/doctorServices.js';
import {openModal} from './components/modals.js';
import {createDoctorCard} from './components/doctorCard.js';
import {patientSignup, patientLogin} from './services/patientServices.js';


// ── Bootstrap on page load ────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    loadDoctorCards();

    // Modal trigger buttons are rendered by header.js; safe to bind here
    const signupBtn = document.getElementById("patientSignup");
    if (signupBtn) signupBtn.addEventListener("click", () => openModal("patientSignup"));

    const loginBtn = document.getElementById("patientLogin");
    if (loginBtn) loginBtn.addEventListener("click", () => openModal("patientLogin"));
});

// ── Search + filter listeners ─────────────────────────────────────────────────
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

// ── Load all doctor cards ─────────────────────────────────────────────────────
function loadDoctorCards() {
    getDoctors()
        .then(doctors => {
            const contentDiv = document.getElementById("content");
            contentDiv.innerHTML = "";
            doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
        })
        .catch(error => console.error("Failed to load doctors:", error));
}

// ── Filter doctors on search/dropdown change ──────────────────────────────────
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

// ── Utility: render a pre-fetched list of doctors ─────────────────────────────
export function renderDoctorCards(doctors) {
    const contentDiv = document.getElementById("content");
    contentDiv.innerHTML = "";
    doctors.forEach(doctor => contentDiv.appendChild(createDoctorCard(doctor)));
}

// ── Patient signup (called from modal submit button) ──────────────────────────
window.signupPatient = async function () {
    try {
        const name = document.getElementById("name").value;
        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;
        const phone = document.getElementById("phone").value;
        const address = document.getElementById("address").value;

        const {success, message} = await patientSignup({name, email, password, phone, address});
        if (success) {
            alert(message);
            document.getElementById("modal").style.display = "none";
            window.location.reload();
        } else {
            alert(message);
        }
    } catch (error) {
        console.error("Signup failed:", error);
        alert("❌ An error occurred while signing up.");
    }
};

// ── Patient login (called from modal submit button) ───────────────────────────
window.loginPatient = async function () {
    try {
        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;

        const response = await patientLogin({email, password});
        if (response.ok) {
            const result = await response.json();
            localStorage.setItem("token", result.token);
            selectRole("loggedPatient");
            window.location.href = "/pages/loggedPatientDashboard.html";
        } else {
            alert("❌ Invalid credentials!");
        }
    } catch (error) {
        console.error("Error :: loginPatient ::", error);
        alert("❌ Failed to login. Please try again.");
    }
};
