package com.cinoteck.application.views;

public interface AuthService {
	 boolean login(String username, String password);
	    void logout();
	    boolean isAuthenticated();
}
