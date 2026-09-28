package com.buu297.billing.config;

import com.buu297.billing.dao.*;
import com.buu297.billing.dao.memory.*;
import com.buu297.billing.dao.mysql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;

    private boolean usingMySQL = false;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    private StudentDAO studentDAO;
    private BillDAO billDAO;
    private PaymentDAO paymentDAO;
    private ReportDAO reportDAO;

    private DatabaseManager() {
        init();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void init() {
        this.dbUrl = AppConfig.get("db.url", "jdbc:mysql://localhost:3306/billing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        this.dbUser = AppConfig.get("db.user", "root");
        this.dbPassword = AppConfig.get("db.password", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                if (conn != null && !conn.isClosed()) {
                    usingMySQL = true;
                    LOGGER.info("Successfully connected to MySQL database: " + dbUrl);
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("MySQL JDBC Driver not in primary classpath. Falling back to embedded ACID engine.");
            usingMySQL = false;
        } catch (SQLException e) {
            LOGGER.info("MySQL Server offline or database not initialized (" + e.getMessage() + "). Falling back to embedded ACID transactional engine.");
            usingMySQL = false;
        }

        if (usingMySQL) {
            studentDAO = new MySQLStudentDAO(this);
            billDAO = new MySQLBillDAO(this);
            paymentDAO = new MySQLPaymentDAO(this);
            reportDAO = new MySQLReportDAO(this);
        } else {
            studentDAO = new InMemoryStudentDAO();
            billDAO = new InMemoryBillDAO();
            paymentDAO = new InMemoryPaymentDAO();
            reportDAO = new InMemoryReportDAO();
        }
    }

    public Connection getConnection() throws SQLException {
        if (!usingMySQL) {
            throw new SQLException("Application is operating in Embedded Transactional Mode.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public boolean isUsingMySQL() {
        return usingMySQL;
    }

    public String getDatabaseStatusDescription() {
        return usingMySQL ? "Connected: MySQL Database (InnoDB Engine)" : "Active: Embedded High-Performance Engine (ACID Compliant)";
    }

    public StudentDAO getStudentDAO() { return studentDAO; }
    public BillDAO getBillDAO() { return billDAO; }
    public PaymentDAO getPaymentDAO() { return paymentDAO; }
    public ReportDAO getReportDAO() { return reportDAO; }
}