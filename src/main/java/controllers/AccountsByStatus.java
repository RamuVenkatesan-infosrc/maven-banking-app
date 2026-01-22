================================================================================
FIXED CODE FOR: src/main/java/controllers/AccountsByStatus.java
================================================================================
package controllers;

import java.sql.Connection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.owasp.encoder.Encode;

import dao.BankDAOImpl;
import models.Account;
import models.Message;
import models.User;
import util.ConnectionFactory;

public class AccountsByStatus {
    public static void getAccountsByStatus(HttpServletRequest req, HttpServletResponse res) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            BankDAOImpl bDao = new BankDAOImpl(conn);
            ObjectMapper om = new ObjectMapper();
            String[] params = req.getRequestURI().split("/");
            HttpSession session = req.getSession(false);
            User currentUser = null;

            if (session == null) {
                sendUnauthorizedResponse(res, om);
            } else {
                currentUser = (User) session.getAttribute("user");
                if (currentUser != null && (currentUser.getRole().getRoleId() == 1 || currentUser.getRole().getRoleId() == 2)) {
                    int status = validateAndParseStatus(params);
                    if (status == -1) {
                        sendBadRequestResponse(res, om);
                    } else {
                        List<Account> accounts = bDao.getAccountsByStatus(status);
                        sendSuccessResponse(res, om, accounts);
                    }
                } else {
                    sendUnauthorizedResponse(res, om);
                }
            }

            // Set Content Security Policy header
            res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';");
        } catch (Exception e) {
            sendErrorResponse(res, e);
        }
    }

    private static int validateAndParseStatus(String[] params) {
        if (params.length < 4) {
            return -1;
        }
        try {
            return Integer.parseInt(params[3]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void sendUnauthorizedResponse(HttpServletResponse res, ObjectMapper om) throws Exception {
        res.setStatus(401);
        res.getWriter().write(om.writeValueAsString(new Message("The requested action is not permitted")));
    }

    private static void sendBadRequestResponse(HttpServletResponse res, ObjectMapper om) throws Exception {
        res.setStatus(400);
        res.getWriter().write(om.writeValueAsString(new Message("Invalid status parameter")));
    }

    private static void sendSuccessResponse(HttpServletResponse res, ObjectMapper om, List<Account> accounts) throws Exception {
        res.setStatus(200);
        res.setContentType("application/json");
        res.getWriter().write(Encode.forHtml(om.writeValueAsString(accounts)));
    }

    private static void sendErrorResponse(HttpServletResponse res, Exception e) {
        try {
            res.setStatus(500);
            res.getWriter().write(Encode.forHtml("An error occurred: " + e.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}


================================================================================
FIXED CODE FOR: src/main/java/dao/BankDAOImpl.java
================================================================================
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import models.Account;
import models.AccountLink;
import models.AccountStatus;
import models.AccountType;
import models.Role;
import models.User;
import org.owasp.encoder.Encode;

public class BankDAOImpl implements BankDAO {
    private Connection conn;

    public BankDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public int insertUser(User user) {
        String sql = "INSERT INTO users (username, password, first_name, last_name, email, role_id) VALUES (?,?,?,?,?,?)";
        int insertId = 0;
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sanitizeInput(user.getUsername()));
            ps.setString(2, sanitizeInput(user.getPassword()));
            ps.setString(3, sanitizeInput(user.getFirstName()));
            ps.setString(4, sanitizeInput(user.getLastName()));
            ps.setString(5, sanitizeInput(user.getEmail()));
            ps.setInt(6, user.getRole().getRoleId());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                insertId = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return insertId;
    }

    @Override
    public int insertAccount(Account account) {
        String sql = "INSERT INTO accounts (balance, status_id, type_id) VALUES (?,?,?)";
        int insertId = 0;
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setDouble(1, account.getBalance());
            ps.setInt(2, account.getStatus().getStatusId());
            ps.setInt(3, account.getType().getTypeId());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                insertId = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return insertId;
    }

    @Override
    public int insertAccountLink(AccountLink link) {
        String sql = "INSERT INTO account_links (user_id, account_id) VALUES (?,?)";
        int insertId = 0;
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, link.getUserId());
            ps.setInt(2, link.getAccountId());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                insertId = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return insertId;
    }

    @Override
    public List<Account> getAllAccounts() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT accounts.account_id, accounts.balance, account_status.status_id, account_status.status, account_type.type_id, account_type.type FROM accounts INNER JOIN account_status ON accounts.status_id = account_status.status_id INNER JOIN account_type ON accounts.type_id = account_type.type_id ORDER BY accounts.account_id ASC";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                accounts.add(new Account(
                        rs.getInt(1),
                        rs.getDouble(2),
                        new AccountStatus(rs.getInt(3), sanitizeInput(rs.getString(4))),
                        new AccountType(rs.getInt(5), sanitizeInput(rs.getString(6)))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    @Override
    public List<Account> getAccountsByUser(int userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT accounts.account_id, accounts.balance, account_status.status_id, account_status.status, account_type.type_id, account_type.type FROM account_links INNER JOIN accounts ON account_links.account_id = accounts.account_id INNER JOIN account_status ON accounts.status_id = account_status.status_id INNER JOIN account_type ON accounts.type_id = account_type.type_id WHERE account_links.user_id = ? ORDER BY accounts.account_id ASC";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                accounts.add(new Account(
                        rs.getInt(1),
                        rs.getDouble(2),
                        new AccountStatus(rs.getInt(3), sanitizeInput(rs.getString(4))),
                        new AccountType(rs.getInt(5), sanitizeInput(rs.getString(6)))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    @Override
    public List<Account> getAccountsByStatus(int statusId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT accounts.account_id, accounts.balance, account_status.status_id, account_status.status, account_type.type_id, account_type.type FROM accounts INNER JOIN account_status ON accounts.status_id = account_status.status_id INNER JOIN account_type ON accounts.type_id = account_type.type_id WHERE accounts.status_id = ? ORDER BY accounts.account_id ASC";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, statusId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                accounts.add(new Account(
                        rs.getInt(1),
                        rs.getDouble(2),
                        new AccountStatus(rs.getInt(3), sanitizeInput(rs.getString(4))),
                        new AccountType(rs.getInt(5), sanitizeInput(rs.getString(6)))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    @Override
    public List<Account> getAllSavingsAccounts() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT accounts.account_id, accounts.balance, accounts.status_id, account_status.status, accounts.type_id, account_type.type FROM accounts INNER JOIN account_status ON accounts.status_id = account_status.status_id INNER JOIN account_type ON accounts.type_id = account_type.type_id WHERE accounts.type_id = 2";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                accounts.add(new Account(
                        rs.getInt(1),
                        rs.getDouble(2),
                        new AccountStatus(rs.getInt(3), sanitizeInput(rs.getString(4))),
                        new AccountType(rs.getInt(5), sanitizeInput(rs.getString(6)))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    @Override
    public Account getAccountById(int accountId) {
        String sql = "SELECT accounts.account_id, accounts.balance, account_status.status_id, account_status.status, account_type.type_id, account_type.type FROM accounts INNER JOIN account_status ON accounts.status_id = account_status.status_id INNER JOIN account_type ON accounts.type_id = account_type.type_id WHERE accounts.account_id = ?";
        Account account = new Account();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                account.setAccountId(rs.getInt(1));
                account.setBalance(rs.getDouble(2));
                account.setStatus(new AccountStatus(rs.getInt(3), sanitizeInput(rs.getString(4))));
                account.setType(new AccountType(rs.getInt(5), sanitizeInput(rs.getString(6))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return account;
    }

    @Override
    public List<AccountStatus> getAllStatuses() {
        List<AccountStatus> statuses = new ArrayList<>();
        String sql = "SELECT * FROM account_status";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                statuses.add(new AccountStatus(rs.getInt(1), sanitizeInput(rs.getString(2))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return statuses;
    }

    @Override
    public List<AccountType> getAllTypes() {
        List<AccountType> types = new ArrayList<>();
        String sql = "SELECT * FROM account_type";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                types.add(new AccountType(rs.getInt(1), sanitizeInput(rs.getString(2))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return types;
    }

    @Override
    public AccountLink getLinkById(int id) {
        AccountLink link = new AccountLink();
        String sql = "SELECT * FROM account_links where accountlink_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                link.setId(rs.getInt(1));
                link.setUserId(rs.getInt(2));
                link.setAccountId(rs.getInt(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return link;
    }

    @Override
    public int getLinkByUserAndAcct(int userId, int accountId) {
        int linkId = 0;
        String sql = "SELECT accountlink_id FROM account_links WHERE user_id = ? AND account_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                linkId = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return linkId;
    }

    @Override
    public List<AccountLink> getLinksByUser(int userId) {
        List<AccountLink> links = new ArrayList<>();
        String sql = "SELECT * FROM account_links WHERE user_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                links.add(new AccountLink(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return links;
    }

    @Override
    public List<AccountLink> getLinksByAccount(int accountId) {
        List<AccountLink> links = new ArrayList<>();
        String sql = "SELECT * FROM account_links WHERE account_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                links.add(new AccountLink(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return links;
    }

    @Override
    public List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM roles";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roles.add(new Role(rs.getInt(1), sanitizeInput(rs.getString(2))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT users.user_id, users.username, users.password, users.first_name, users.last_name, users.email, roles.role_id, roles.role FROM users INNER JOIN roles ON users.role_id = roles.role_id ORDER BY users.user_id ASC";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(new User(
                        rs.getInt(1),
                        sanitizeInput(rs.getString(2)),
                        sanitizeInput(rs.getString(3)),
                        sanitizeInput(rs.getString(4)),
                        sanitizeInput(rs.getString(5)),
                        sanitizeInput(rs.getString(6)),
                        new Role(rs.getInt(7), sanitizeInput(rs.getString(8)))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public List<User> getUsersByAccount(int accountId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT users.user_id, users.username, users.password, users.first_name, users.last_name, users.email, roles.role_id, roles.role FROM account_links INNER JOIN users ON account_links.user_id = users.user_id INNER JOIN roles ON users.role_id = roles.role_id WHERE account_links.account_id = ? ORDER BY users.user_id ASC";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(new User(
                        rs.getInt(1),
                        sanitizeInput(rs.getString(2)),
                        sanitizeInput(rs.getString(3)),
                        sanitizeInput(rs.getString(4)),
                        sanitizeInput(rs.getString(5)),
                        sanitizeInput(rs.getString(6)),
                        new Role(rs.getInt(7), sanitizeInput(rs.getString(8)))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public User getUserById(int id) {
        User user = new User();
        String sql = "SELECT users.user_id, users.username, users.password, users.first_name, users.last_name, users.email, roles.role_id, roles.role FROM users INNER JOIN roles ON users.role_id = roles.role_id WHERE users.user_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();