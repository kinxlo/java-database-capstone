## MySQL Database Design

Relational storage is used for core operational data that requires strong consistency,
joins, and transactional integrity.

### Table: patients
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- patient_code: VARCHAR(30), Not Null, Unique
- first_name: VARCHAR(100), Not Null
- last_name: VARCHAR(100), Not Null
- date_of_birth: DATE, Not Null
- gender: ENUM('MALE','FEMALE','OTHER','PREFER_NOT_TO_SAY'), Null
- email: VARCHAR(255), Not Null, Unique
- phone: VARCHAR(20), Not Null, Unique
- address_line1: VARCHAR(255), Null
- address_line2: VARCHAR(255), Null
- city: VARCHAR(120), Null
- emergency_contact_name: VARCHAR(150), Null
- emergency_contact_phone: VARCHAR(20), Null
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP
- updated_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- deleted_at: TIMESTAMP, Null

Notes:
- Use soft delete (`deleted_at`) to preserve medical history.
- Email and phone format validation should be done in application code, not only DB.

### Table: doctors
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- doctor_code: VARCHAR(30), Not Null, Unique
- first_name: VARCHAR(100), Not Null
- last_name: VARCHAR(100), Not Null
- email: VARCHAR(255), Not Null, Unique
- phone: VARCHAR(20), Not Null, Unique
- license_number: VARCHAR(80), Not Null, Unique
- specialization: VARCHAR(120), Not Null
- years_of_experience: TINYINT UNSIGNED, Null
- is_active: BOOLEAN, Not Null, Default TRUE
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP
- updated_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- deleted_at: TIMESTAMP, Null

### Table: admin
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- username: VARCHAR(80), Not Null, Unique
- full_name: VARCHAR(150), Not Null
- email: VARCHAR(255), Not Null, Unique
- password_hash: VARCHAR(255), Not Null
- role: ENUM('SUPER_ADMIN','STAFF_ADMIN'), Not Null, Default 'STAFF_ADMIN'
- is_active: BOOLEAN, Not Null, Default TRUE
- last_login_at: TIMESTAMP, Null
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP
- updated_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### Table: clinic_locations
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- location_code: VARCHAR(30), Not Null, Unique
- name: VARCHAR(120), Not Null
- address_line1: VARCHAR(255), Not Null
- address_line2: VARCHAR(255), Null
- city: VARCHAR(120), Not Null
- state: VARCHAR(120), Null
- postal_code: VARCHAR(20), Null
- phone: VARCHAR(20), Null
- timezone: VARCHAR(64), Not Null, Default 'UTC'
- is_active: BOOLEAN, Not Null, Default TRUE
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP
- updated_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### Table: doctor_availability
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- doctor_id: BIGINT UNSIGNED, Not Null, Foreign Key -> doctors(id)
- clinic_location_id: BIGINT UNSIGNED, Not Null, Foreign Key -> clinic_locations(id)
- day_of_week: TINYINT UNSIGNED, Not Null  // 1 = Monday ... 7 = Sunday
- start_time: TIME, Not Null
- end_time: TIME, Not Null
- slot_minutes: SMALLINT UNSIGNED, Not Null, Default 30
- is_available: BOOLEAN, Not Null, Default TRUE
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP
- updated_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Constraints and indexes:
- CHECK (start_time < end_time)
- CHECK (slot_minutes IN (15, 20, 30, 45, 60))
- UNIQUE (doctor_id, clinic_location_id, day_of_week, start_time, end_time)

### Table: appointments
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- appointment_code: VARCHAR(40), Not Null, Unique
- doctor_id: BIGINT UNSIGNED, Not Null, Foreign Key -> doctors(id)
- patient_id: BIGINT UNSIGNED, Not Null, Foreign Key -> patients(id)
- clinic_location_id: BIGINT UNSIGNED, Not Null, Foreign Key -> clinic_locations(id)
- start_time: DATETIME, Not Null
- end_time: DATETIME, Not Null
- status: ENUM('SCHEDULED','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW'), Not Null, Default 'SCHEDULED'
- reason: VARCHAR(500), Null
- created_by_admin_id: BIGINT UNSIGNED, Null, Foreign Key -> admin(id)
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP
- updated_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- cancelled_at: TIMESTAMP, Null

Constraints and indexes:
- CHECK (start_time < end_time)
- INDEX idx_appointments_patient_time (patient_id, start_time)
- INDEX idx_appointments_doctor_time (doctor_id, start_time)

Scheduling rule:
- A doctor must not have overlapping active appointments.
- Enforce in service-layer transaction: before insert/update, reject if another
  appointment exists for the same doctor where `(existing.start_time < new.end_time)`
  and `(existing.end_time > new.start_time)` and status not in ('CANCELLED').

### Table: payments
- id: BIGINT UNSIGNED, Primary Key, Auto Increment
- appointment_id: BIGINT UNSIGNED, Not Null, Foreign Key -> appointments(id)
- patient_id: BIGINT UNSIGNED, Not Null, Foreign Key -> patients(id)
- amount: DECIMAL(12,2), Not Null
- currency: CHAR(3), Not Null, Default 'USD'
- method: ENUM('CASH','CARD','TRANSFER','INSURANCE'), Not Null
- status: ENUM('PENDING','PAID','FAILED','REFUNDED'), Not Null, Default 'PENDING'
- reference_code: VARCHAR(80), Null, Unique
- paid_at: TIMESTAMP, Null
- created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP

### Foreign Key policy decisions
- `appointments.patient_id` -> `patients.id`: ON DELETE RESTRICT
- `appointments.doctor_id` -> `doctors.id`: ON DELETE RESTRICT
- `appointments.clinic_location_id` -> `clinic_locations.id`: ON DELETE RESTRICT
- `appointments.created_by_admin_id` -> `admin.id`: ON DELETE SET NULL
- `doctor_availability.doctor_id` -> `doctors.id`: ON DELETE CASCADE
- `doctor_availability.clinic_location_id` -> `clinic_locations.id`: ON DELETE CASCADE
- `payments.appointment_id` -> `appointments.id`: ON DELETE RESTRICT
- `payments.patient_id` -> `patients.id`: ON DELETE RESTRICT

Rationale:
- We retain appointment and payment history for compliance and audit needs.
- Soft delete for people records avoids accidental medical-history loss.

## MongoDB Collection Design

MongoDB is used for flexible, evolving, and semi-structured data.

### Collection: prescriptions

```json
{
  "_id": { "$oid": "64abc1234567890def000001" },
  "prescriptionId": "RX-2026-000451",
  "appointmentId": 1051,
  "patientId": 320,
  "doctorId": 78,
  "issuedAt": { "$date": "2026-04-18T09:30:00Z" },
  "status": "ACTIVE",
  "diagnosis": ["upper_respiratory_infection"],
  "medications": [
	{
	  "name": "Amoxicillin",
	  "dosage": "500mg",
	  "frequency": "3 times daily",
	  "durationDays": 7,
	  "instructions": "Take after meals",
	  "refillCount": 0
	},
	{
	  "name": "Ibuprofen",
	  "dosage": "400mg",
	  "frequency": "as needed",
	  "durationDays": 3,
	  "instructions": "Take with water",
	  "refillCount": 1
	}
  ],
  "doctorNotes": {
	"subjective": "Patient reports sore throat for 3 days",
	"objective": "Mild fever, throat redness",
	"plan": "Review in one week if symptoms persist"
  },
  "attachments": [
	{
	  "type": "lab_report",
	  "fileUrl": "https://files.example.com/labs/lab-9981.pdf",
	  "uploadedAt": { "$date": "2026-04-18T09:40:00Z" }
	}
  ],
  "tags": ["urgent", "follow-up"],
  "metadata": {
	"schemaVersion": 1,
	"source": "doctor_portal",
	"lastUpdatedBy": "doctor:78"
  },
  "createdAt": { "$date": "2026-04-18T09:32:00Z" },
  "updatedAt": { "$date": "2026-04-18T09:40:00Z" }
}
```

Design decisions:
- Store only relational references (`appointmentId`, `patientId`, `doctorId`) rather
  than full patient/doctor objects to reduce duplication and stale data risk.
- Keep flexible fields (`doctorNotes`, `attachments`, `tags`, `metadata`) in MongoDB
  because they evolve faster than strict relational schemas.
- Prescription is tied to an appointment by default (`appointmentId`) for traceability.
  If future requirements allow independent prescriptions, keep `appointmentId` nullable
  and add `originType`/`originReference` fields.

Recommended MongoDB indexes:
- `{ prescriptionId: 1 }` unique
- `{ appointmentId: 1 }`
- `{ patientId: 1, issuedAt: -1 }`
- `{ doctorId: 1, issuedAt: -1 }`
- `{ "tags": 1 }` (multikey)

Schema evolution note:
- Include `metadata.schemaVersion` so future changes can be handled with backward-
  compatible readers or migration scripts.
