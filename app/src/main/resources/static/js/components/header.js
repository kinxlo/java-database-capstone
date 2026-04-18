// header.js
// Dynamically renders the site header based on the current user role and session token.
// Roles: admin | doctor | patient | loggedPatient | (none = root / index page)

function renderHeader() {
    const headerDiv = document.getElementById("header");
    if (!headerDiv) return;

    // ── Root page: clear role + token, show logo-only header ──────────────────
    if (window.location.pathname === "/" || window.location.pathname.endsWith("index.html")) {
        localStorage.removeItem("userRole");
        localStorage.removeItem("token");
        headerDiv.innerHTML = `
      <header class="header">
        <a class="logo-link" href="/">
          <img src="./assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </a>
      </header>`;
        return;
    }

    const role = localStorage.getItem("userRole");
    const token = localStorage.getItem("token");

    // ── Guard: authenticated roles without a token → session expired ──────────
    if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
        localStorage.removeItem("userRole");
        alert("Session expired or invalid login. Please log in again.");
        window.location.href = "/";
        return;
    }

    // ── Build base header with logo ───────────────────────────────────────────
    let headerContent = `
    <header class="header">
      <a class="logo-link" href="/">
        <img src="../assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
        <span class="logo-title">Hospital CMS</span>
      </a>
      <nav>`;

    if (role === "admin") {
        headerContent += `
        <button id="addDocBtn" class="adminBtn" onclick="openModal('addDoctor')">Add Doctor</button>
        <a href="#" onclick="logout()">Logout</a>`;
    } else if (role === "doctor") {
        headerContent += `
        <button class="adminBtn" onclick="selectRole('doctor')">Home</button>
        <a href="#" onclick="logout()">Logout</a>`;
    } else if (role === "patient") {
        headerContent += `
        <button id="patientLogin"  class="adminBtn">Login</button>
        <button id="patientSignup" class="adminBtn">Sign Up</button>`;
    } else if (role === "loggedPatient") {
        headerContent += `
        <button class="adminBtn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
        <button class="adminBtn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
        <a href="#" onclick="logoutPatient()">Logout</a>`;
    }

    headerContent += `
      </nav>
    </header>`;

    headerDiv.innerHTML = headerContent;
    attachHeaderButtonListeners();
}

// ── Attach click listeners to dynamically rendered login buttons ──────────────
function attachHeaderButtonListeners() {
    const patientLoginBtn = document.getElementById("patientLogin");
    const patientSignupBtn = document.getElementById("patientSignup");

    if (patientLoginBtn) patientLoginBtn.addEventListener("click", () => openModal("patientLogin"));
    if (patientSignupBtn) patientSignupBtn.addEventListener("click", () => openModal("patientSignup"));
}

// ── Auth helpers ──────────────────────────────────────────────────────────────
function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    window.location.href = "/";
}

function logoutPatient() {
    localStorage.removeItem("token");
    // Keep role as "patient" so the patient dashboard shows Login / Sign Up again
    localStorage.setItem("userRole", "patient");
    window.location.href = "/pages/patientDashboard.html";
}

// ── Auto-run ──────────────────────────────────────────────────────────────────
renderHeader();
