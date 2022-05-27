package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.Value;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class UserDetails {
	String name;
	String email;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
}
