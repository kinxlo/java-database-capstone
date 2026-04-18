// appointmentRecordService.js
import { API_BASE_URL } from "../config/config.js";
const APPOINTMENT_API = `${API_BASE_URL}/appointments`;

// Fetch all appointments for the doctor dashboard (filtered by date + patient name)
export async function getAllAppointments(date, patientName, token) {
  const response = await fetch(`${APPOINTMENT_API}/${date}/${patientName}/${token}`);
  if (!response.ok) {
    throw new Error("Failed to fetch appointments");
  }
  return await response.json();
}

// Fetch the logged-in patient's appointment list (used by appointmentRecord.js)
export async function getAppointmentRecord(token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/patient/${token}`);
    if (!response.ok) throw new Error("Failed to fetch appointment record");
    const data = await response.json();
    return data.appointments ?? [];
  } catch (error) {
    console.error("Error :: getAppointmentRecord ::", error);
    return [];
  }
}

export async function bookAppointment(appointment, token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/${token}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || "Something went wrong"
    };
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}

export async function updateAppointment(appointment, token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/${token}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || "Something went wrong"
    };
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}
