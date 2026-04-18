package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctor")
public class Doctor {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(min = 3, max = 100)
	@Column(nullable = false, length = 100)
	private String name;

	@NotBlank
	@Size(min = 3, max = 50)
	@Column(nullable = false, length = 50)
	private String specialty;

	@NotBlank
	@Email
	@Size(max = 255)
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@NotBlank
	@Size(min = 8, max = 255)
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column(nullable = false, length = 255)
	private String password;

	@NotBlank
	@Pattern(regexp = "^[0-9]{10}$")
	@Column(nullable = false, unique = true, length = 20)
	private String phone;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "doctor_available_times", joinColumns = @JoinColumn(name = "doctor_id"))
	@Column(name = "time_slot", nullable = false, length = 30)
	private List<String> availableTimes = new ArrayList<>();

	public Doctor() {
	}

	public Doctor(String name, String specialty, String email, String password, String phone) {
		this.name = name;
		this.specialty = specialty;
		this.email = email;
		this.password = password;
		this.phone = phone;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSpecialty() {
		return specialty;
	}

	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public List<String> getAvailableTimes() {
		return availableTimes;
	}

	public void setAvailableTimes(List<String> availableTimes) {
		this.availableTimes = availableTimes;
	}

}

