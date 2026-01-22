================================================================================
FIXED CODE FOR: src/main/java/controllers/Users.java
================================================================================
package controllers;

import java.sql.Connection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.BankDAOImpl;
import models.Message;
import models.Role;
import models.User;
import util.ConnectionFactory;
import org.owasp.encoder.Encode;

public class Users {
    public static void getAllUsers(HttpServletRequest req, HttpServletResponse res) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            BankDAOImpl bDao = new BankDAOImpl(conn);
            ObjectMapper om = new ObjectMapper();
            HttpSession session = req.getSession(false);
            User currentUser = null;
            if (session == null) {
                res.setStatus(401);
                res.getWriter().write(om.writeValueAsString(new Message(Encode.forHtml("The requested action is not permitted"))));
            } else {
                currentUser = (User) session.getAttribute("user");
                if (currentUser.getRole().getRoleId() == 1 || currentUser.getRole().getRoleId() == 2) {
                    List<User> users = bDao.getAllUsers();
                    for (User user : users) {
                        user.setUsername(Encode.forHtml(user.getUsername()));
                        user.setFirstName(Encode.forHtml(user.getFirstName()));
                        user.setLastName(Encode.forHtml(user.getLastName()));
                        user.setEmail(Encode.forHtml(user.getEmail()));
                    }
                    res.setStatus(200);
                    res.getWriter().write(om.writeValueAsString(users));
                } else {
                    res.setStatus(401);
                    res.getWriter().write(om.writeValueAsString(new Message(Encode.forHtml("The requested action is not permitted"))));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                res.setStatus(500);
                res.getWriter().write(new ObjectMapper().writeValueAsString(new Message(Encode.forHtml("An internal server error occurred"))));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static void updateUser(HttpServletRequest req, HttpServletResponse res) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            BankDAOImpl bDao = new BankDAOImpl(conn);
            ObjectMapper om = new ObjectMapper();
            JsonNode jsonNode = om.readTree(req.getReader());
            HttpSession session = req.getSession(false);
            User currentUser = null;
            if (session == null) {
                res.setStatus(401);
                res.getWriter().write(om.writeValueAsString(new Message(Encode.forHtml("The requested action is not permitted"))));
            } else {
                User testUser = bDao.getUserByUsername(Encode.forHtml(jsonNode.get("username").asText()));
                if ((testUser.getUserId() == 0) || (testUser.getUserId() == jsonNode.get("userId").asInt())) {
                    User user = new User(
                        jsonNode.get("userId").asInt(),
                        Encode.forHtml(jsonNode.get("username").asText()),
                        Encode.forHtml(jsonNode.get("password").asText()),
                        Encode.forHtml(jsonNode.get("firstName").asText()),
                        Encode.forHtml(jsonNode.get("lastName").asText()),
                        Encode.forHtml(jsonNode.get("email").asText()),
                        new Role(jsonNode.get("roleId").asInt(), "blah")
                    );
                    currentUser = (User) session.getAttribute("user");
                    if (currentUser.getRole().getRoleId() == 1 || currentUser.getUserId() == user.getUserId()) {
                        bDao.updateUser(user);
                        if (currentUser.getUserId() == user.getUserId()) {
                            session.removeAttribute("user");
                            session.setAttribute("user", bDao.getUserById(user.getUserId()));
                        }
                        res.setStatus(200);
                        res.getWriter().write(om.writeValueAsString(bDao.getUserById(user.getUserId())));
                    } else {
                        res.setStatus(401);
                        res.getWriter().write(om.writeValueAsString(new Message(Encode.forHtml("The requested action is not permitted"))));
                    }
                } else {
                    res.setStatus(400);
                    res.getWriter().write(om.writeValueAsString(new Message(Encode.forHtml("Username already in use"))));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                res.setStatus(500);
                res.getWriter().write(new ObjectMapper().writeValueAsString(new Message(Encode.forHtml("An internal server error occurred"))));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}