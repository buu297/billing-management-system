package com.buu297.billing.server;

import com.buu297.billing.config.AppConfig;
import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.PaymentDAO;
import com.buu297.billing.dao.ReportDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.*;
import com.buu297.billing.service.ConcurrencyPaymentService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class StudentApiServer {
    private static final Logger LOGGER = Logger.getLogger(StudentApiServer.class.getName());
    private final int port;
    private final File webRoot;
    private HttpServer server;
    private final ConcurrencyPaymentService paymentService;
    private final StudentDAO studentDAO;
    private final BillDAO billDAO;
    private final PaymentDAO paymentDAO;
    private final ReportDAO reportDAO;

    public StudentApiServer(int port, File webRoot) {
        this.port = port;
        this.webRoot = webRoot;
        this.paymentService = new ConcurrencyPaymentService();
        this.studentDAO = DatabaseManager.getInstance().getStudentDAO();
        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.paymentDAO = DatabaseManager.getInstance().getPaymentDAO();
        this.reportDAO = DatabaseManager.getInstance().getReportDAO();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/students", this::handleStudents);
        server.createContext("/api/bills", this::handleBills);
        server.createContext("/api/payments", this::handlePayments);
        server.createContext("/api/reports/summary", this::handleReportsSummary);
        server.createContext("/api/concurrency-test", this::handleConcurrencyTest);
        server.createContext("/api/status", this::handleStatus);

        // Static files for web dashboard
        server.createContext("/", new StaticFileHandler(webRoot));

        server.setExecutor(null); // default executor
        server.start();
        LOGGER.info("Student Billing Web Dashboard & REST Server running at http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        String json = "{\"status\":\"ONLINE\",\"database\":\"" + JsonUtil.escape(DatabaseManager.getInstance().getDatabaseStatusDescription()) + "\"}";
        sendJsonResponse(exchange, 200, json);
    }

    private void handleStudents(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        List<Student> students = studentDAO.findAll();
        sendJsonResponse(exchange, 200, JsonUtil.studentsToJson(students));
    }

    private void handleBills(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());

        if (path.startsWith("/api/bills/") && path.length() > "/api/bills/".length()) {
            String billId = path.substring("/api/bills/".length());
            Optional<Bill> bill = billDAO.findById(billId);
            if (bill.isPresent()) {
                sendJsonResponse(exchange, 200, JsonUtil.billToJson(bill.get()));
            } else {
                sendJsonResponse(exchange, 404, "{\"error\":\"Bill not found\"}");
            }
            return;
        }

        String studentId = query.get("studentId");
        List<Bill> list;
        if (studentId != null && !studentId.isEmpty()) {
            list = billDAO.findByStudentId(studentId);
        } else {
            list = billDAO.findAll();
        }
        sendJsonResponse(exchange, 200, JsonUtil.billsToJson(list));
    }

    private void handlePayments(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);

            String billId = data.get("billId");
            String studentId = data.get("studentId");
            String amountStr = data.get("amount");
            String methodStr = data.get("method");
            String notes = data.get("notes");

            if (billId == null || studentId == null || amountStr == null) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Missing required fields: billId, studentId, amount\"}");
                return;
            }

            try {
                BigDecimal amount = new BigDecimal(amountStr);
                PaymentMethod method = PaymentMethod.fromString(methodStr != null ? methodStr : "CREDIT_CARD");
                TransactionResult result = paymentService.processPayment(billId, studentId, amount, method, notes);
                sendJsonResponse(exchange, 200, JsonUtil.transactionResultToJson(result));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"Error: " + JsonUtil.escape(e.getMessage()) + "\"}");
            }
            return;
        }

        // GET method
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
        String billId = query.get("billId");
        String studentId = query.get("studentId");
        List<Payment> list;
        if (billId != null && !billId.isEmpty()) {
            list = paymentDAO.findByBillId(billId);
        } else if (studentId != null && !studentId.isEmpty()) {
            list = paymentDAO.findByStudentId(studentId);
        } else {
            list = paymentDAO.findRecent(100);
        }
        sendJsonResponse(exchange, 200, JsonUtil.paymentsToJson(list));
    }

    private void handleReportsSummary(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
        String sem = query.get("semester");
        String dept = query.get("department");
        String status = query.get("status");

        BillingReportSummary summary = reportDAO.generateSummary(sem, dept, status, null, null);
        sendJsonResponse(exchange, 200, JsonUtil.reportSummaryToJson(summary));
    }

    private void handleConcurrencyTest(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> data = parseSimpleJson(body);
        String billId = data.getOrDefault("billId", "BILL-003");
        String studentId = data.getOrDefault("studentId", "STU202603");
        String amtStr = data.getOrDefault("amount", "200");
        String threadsStr = data.getOrDefault("threads", "5");

        try {
            BigDecimal amt = new BigDecimal(amtStr);
            int threads = Integer.parseInt(threadsStr);
            ConcurrencyPaymentService.ConcurrencySimulationReport rep =
                    paymentService.simulateConcurrentPayments(billId, studentId, amt, threads);

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"billId\":\"").append(JsonUtil.escape(rep.billId)).append("\",");
            sb.append("\"threadCount\":").append(rep.threadCount).append(",");
            sb.append("\"paymentPerThread\":").append(rep.paymentPerThread).append(",");
            sb.append("\"initialBalance\":").append(rep.initialBalance).append(",");
            sb.append("\"finalBalance\":").append(rep.finalBalance).append(",");
            sb.append("\"totalPaidInSim\":").append(rep.totalPaidInSim).append(",");
            sb.append("\"successfulTxns\":").append(rep.successfulTxns).append(",");
            sb.append("\"rejectedTxns\":").append(rep.rejectedTxns).append(",");
            sb.append("\"totalDurationMs\":").append(rep.totalDurationMs).append(",");
            sb.append("\"dataConsistent\":").append(rep.isDataConsistent());
            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[2048];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    map.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } else if (idx == 0) {
                    map.put("", URLDecoder.decode(pair.substring(1), "UTF-8"));
                } else {
                    map.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        String[] tokens = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String token : tokens) {
            String[] kv = token.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }
}
