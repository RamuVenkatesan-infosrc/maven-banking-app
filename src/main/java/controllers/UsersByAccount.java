================================================================================
FIXED CODE FOR: src/main/java/controllers/UsersByAccount.java
================================================================================
package controllers;

import java.sql.Connection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import dao.BankDAOImpl;
import models.User;
import util.ConnectionFactory;
import util.HtmlEncoder;
import util.UserSerializer;

public class UsersByAccount {
    public static void getUsersByAccount(HttpServletRequest req, HttpServletResponse res) {
        try {
            String[] params = req.getRequestURI().split("/");
            if (params.length < 4) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request parameters");
                return;
            }

            int accountId;
            try {
                accountId = Integer.parseInt(params[3]);
            } catch (NumberFormatException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid account ID");
                return;
            }

            Connection conn = ConnectionFactory.getConnection();
            BankDAOImpl bDao = new BankDAOImpl(conn);

            if (!bDao.hasAccountAccess(accountId, req.getSession())) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            List<User> users = bDao.getUsersByAccount(accountId);

            ObjectMapper om = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(User.class, new UserSerializer());
            om.registerModule(module);

            res.setContentType("application/json");
            res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';");
            res.getWriter().write(om.writeValueAsString(users));
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                res.getWriter().write(HtmlEncoder.encode("An error occurred while processing your request."));
            } catch (Exception ex) {
                // If we can't write to the response, log the error
                System.err.println("Error writing error response: " + ex.getMessage());
            }
        }
    }
}


================================================================================
FIXED CODE FOR: src/main/java/models/User.java
================================================================================
package models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import utils.HtmlEncodingSerializer;

@JsonSerialize(using = HtmlEncodingSerializer.class)
public class User {
	private int userId;
	private String username;
	private String password;
	private String firstName;
	private String lastName;
	private String email;
	private Role role;
	
	public User() {
		super();
	}
	
	public User(int userId, String username, String password, String firstName, String lastName, String email, Role role) {
		this.userId = userId;
		this.username = username;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.role = role;
	}
	
	public int getUserId() {
		return this.userId;
	}
	
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public Role getRole() {
		return this.role;
	}
	
	public void setRole(Role role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", username=" + username + ", firstName=" + firstName + ", lastName="
				+ lastName + ", email=" + email + ", role=" + role + "]";
	}
}