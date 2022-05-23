package com.example.googlefit.googlefit.model;

import lombok.Data;
import lombok.ToString;

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
