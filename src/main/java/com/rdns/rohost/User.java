package com.rdns.rohost;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class User {
	private @Id @GeneratedValue Long id;
	private String zonename;
	private String password;
	
	public User() {
		
	}
	public User(String zonename, String password) {
		super();
		this.zonename = zonename;
		this.password = password;
	}
	public String getZonename() {
		return zonename;
	}
	public void setZonename(String zonename) {
		this.zonename = zonename;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((zonename == null) ? 0 : zonename.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (zonename == null) {
			if (other.zonename != null)
				return false;
		} else if (!zonename.equals(other.zonename))
			return false;
		return true;
	}
}
