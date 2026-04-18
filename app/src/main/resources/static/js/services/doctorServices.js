/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/

// doctorServices.js
// All API interactions related to doctor data.

import {API_BASE_URL} from "../config/config.js";

const DOCTOR_API = API_BASE_URL + '/doctor';

/**
 * Fetch all doctors from the backend.
 * @returns {Promise<Array>} Array of doctor objects, or [] on failure.
 */
export async function getDoctors() {
    try {
        const response = await fetch(DOCTOR_API);
        const data = await response.json();
        return data.doctors ?? [];
    } catch (error) {
        console.error("Error :: getDoctors ::", error);
        return [];
    }
}

/**
 * Delete a doctor by ID (admin only).
 * @param {number|string} id    - Doctor's unique identifier.
 * @param {string}        token - Admin authentication token.
 * @returns {Promise<{success: boolean, message: string}>}
 */
export async function deleteDoctor(id, token) {
    try {
        const response = await fetch(`${DOCTOR_API}/${id}/${token}`, {
            method: "DELETE",
        });
        const data = await response.json();
        return {success: response.ok, message: data.message};
    } catch (error) {
        console.error("Error :: deleteDoctor ::", error);
        return {success: false, message: "Failed to delete doctor."};
    }
}

/**
 * Save (create) a new doctor (admin only).
 * @param {Object} doctor - Doctor details (name, email, password, specialty, availableTimes…).
 * @param {string} token  - Admin authentication token.
 * @returns {Promise<{success: boolean, message: string}>}
 */
export async function saveDoctor(doctor, token) {
    try {
        const response = await fetch(`${DOCTOR_API}/${token}`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(doctor),
        });
        const data = await response.json();
        return {success: response.ok, message: data.message};
    } catch (error) {
        console.error("Error :: saveDoctor ::", error);
        return {success: false, message: "Failed to save doctor."};
    }
}

/**
 * Filter doctors by name, available time slot, and/or specialty.
 * Pass null or empty string for any parameter you don't want to filter by.
 * @param {string|null} name      - Doctor name fragment.
 * @param {string|null} time      - Time slot (e.g. "AM" | "PM").
 * @param {string|null} specialty - Medical specialty string.
 * @returns {Promise<{doctors: Array}>} Filtered doctors or { doctors: [] } on failure.
 */
export async function filterDoctors(name, time, specialty) {
    try {
        const safeName = name || "null";
        const safeTime = time || "null";
        const safeSpecialty = specialty || "null";

        const response = await fetch(
            `${DOCTOR_API}/filter/${safeName}/${safeTime}/${safeSpecialty}`
        );

        if (!response.ok) {
            console.error("Error :: filterDoctors :: HTTP", response.status);
            return {doctors: []};
        }

        return await response.json();
    } catch (error) {
        console.error("Error :: filterDoctors ::", error);
        alert("❌ An error occurred while filtering doctors.");
        return {doctors: []};
    }
}
