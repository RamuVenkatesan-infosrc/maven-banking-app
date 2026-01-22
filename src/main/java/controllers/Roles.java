================================================================================
FIXED CODE FOR: src/main/java/controllers/Roles.java
================================================================================
package controllers;

import java.sql.Connection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.json.JsonWriteFeature;

import dao.BankDAOImpl;
import models.Role;
import util.ConnectionFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Roles {
    private static final Logger logger = LogManager.getLogger(Roles.class);

    public static void getRoles(HttpServletRequest req, HttpServletResponse res) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            BankDAOImpl bDao = new BankDAOImpl(conn);
            
            JsonMapper jsonMapper = JsonMapper.builder()
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .build();
            
            List<Role> roles = bDao.getAllRoles();
            
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';");
            
            jsonMapper.writeValue(res.getWriter(), roles);
        } catch (Exception e) {
            logger.error("Error occurred while fetching roles", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                res.getWriter().write("An error occurred while processing your request.");
            } catch (Exception ex) {
                logger.error("Error writing error response", ex);
            }
        }
    }
}