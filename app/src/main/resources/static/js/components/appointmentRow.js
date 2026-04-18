// appointmentRow.js
export function getAppointments(appointment) {
  const tr = document.createElement("tr");

  tr.innerHTML = `
      <td class="patient-id">${appointment.patientName}</td>
      <td>${appointment.doctorName}</td>
      <td>${appointment.date}</td>
      <td>${appointment.time}</td>
      <td><img src="../assets/images/edit/edit.png" alt="action" class="prescription-btn" data-id="${appointment.id}"></img></td>
    `;

  // Attach event listeners
  tr.querySelector(".prescription-btn").addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${appointment.id}&patientName=${encodeURIComponent(appointment.patientName)}`;
  });

  return tr;
}
