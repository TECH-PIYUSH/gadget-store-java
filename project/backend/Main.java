import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

// ============================================================================
// Main.java
// Starts a small built-in Java HTTP server (com.sun.net.httpserver) and wires
// up the REST API. It also serves the frontend files so the whole app runs on
// a single port (http://localhost:8080) with no extra tools.
//
// API endpoints:
//   GET  /api/gadgets  -> list all gadgets (JSON array)
//   POST /api/gadgets  -> add a gadget     (JSON body)
//   GET  /api/orders   -> order history    (JSON array)
//   POST /api/orders   -> place a COD order (JSON body)
// ============================================================================
public class Main {

    private static StoreService service;
    // Folder that holds the frontend files (index.html, styles.css, app.js).
    private static final File WEB_ROOT = new File("../frontend");

    public static void main(String[] args) throws IOException {

        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) { }
        }

        // ---- Storage + inventory setup ----
        FileManager fileManager = new FileManager();
        fileManager.ensureFiles();
        InventoryManager inventory = new InventoryManager();
        service = new StoreService(inventory, fileManager);

        // Load saved gadgets. If nothing is saved yet, seed the 12 samples and save them.
        List<Gadget> saved = fileManager.loadGadgets();
        if (saved.isEmpty()) {
            seedSampleGadgets(inventory);
            fileManager.saveGadgets(inventory.getAllGadgets());
            System.out.println("No saved data found -> seeded 12 sample gadgets into data/gadgets.csv");
        } else {
            for (Gadget g : saved) {
                try { inventory.addGadget(g); } catch (DuplicateSerialException ignored) { }
            }
            System.out.println("Loaded " + inventory.size() + " gadgets from data/gadgets.csv");
        }
        service.loadHistoryFromFile();
        System.out.println("Loaded " + service.getOrderHistory().size() + " past order(s) from data/orders.csv");

        // ---- Start the HTTP server ----
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/gadgets", Main::handleGadgets);
        server.createContext("/api/orders",  Main::handleOrders);
        server.createContext("/",            Main::handleStatic);
        server.setExecutor(null);
        server.start();

        System.out.println("------------------------------------------------------------");
        System.out.println("GadgetStore backend is running.");
        System.out.println("Open your browser at:  http://localhost:" + port);
        System.out.println("Press Ctrl + C to stop the server.");
        System.out.println("------------------------------------------------------------");
    }

    // ---- GET /api/gadgets (list) and POST /api/gadgets (add) ----
    private static void handleGadgets(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        try {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 200, listToJson(service.getInventory().getAllGadgets(), true));

            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> b = Json.parseObject(readBody(ex));
                Gadget g = service.addGadget(
                        text(b, "category"), text(b, "serialNumber"), text(b, "name"), text(b, "brand"),
                        text(b, "specifications"), num(b, "sellingPrice"), num(b, "originalPrice"),
                        whole(b, "stock"), whole(b, "warrantyMonths"));
                sendJson(ex, 201, g.toJson());

            } else {
                sendError(ex, 405, "Method not allowed.");
            }
        } catch (DuplicateSerialException e) {
            sendError(ex, 409, e.getMessage());          // 409 Conflict for duplicate serials
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());          // 400 Bad Request for validation errors
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ---- GET /api/orders (history) and POST /api/orders (place order) ----
    private static void handleOrders(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        try {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 200, listToJson(service.getOrderHistory(), false));

            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> b = Json.parseObject(readBody(ex));
                String fullName = text(b, "fullName");
                if (fullName == null || !fullName.matches("^[a-zA-Z\\s]+$")) {
                sendError(ex, 400, "Customer name must contain only letters and spaces.");
                return;
              }
                Customer customer = new Customer(
                        text(b, "fullName"), text(b, "mobile"), text(b, "email"),
                        text(b, "address"), text(b, "city"), text(b, "pinCode"));
                Order order = service.placeOrder(customer, text(b, "serialNumber"), whole(b, "quantity"));
                sendJson(ex, 201, order.toJson());

            } else {
                sendError(ex, 405, "Method not allowed.");
            }
        } catch (OutOfStockException e) {
            sendError(ex, 400, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ---- Serves the frontend files for normal browser requests ----
    private static void handleStatic(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        File file = new File(WEB_ROOT, path.substring(1));
        if (!file.exists() || file.isDirectory()) {
            sendError(ex, 404, "Not found.");
            return;
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        addCors(ex);
        ex.getResponseHeaders().add("Content-Type", contentType(path));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    // Joins a list of objects (that each have a toJson()) into a JSON array.
    private static String listToJson(List<?> items, boolean gadgets) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            Object o = items.get(i);
            sb.append(gadgets ? ((Gadget) o).toJson() : ((Order) o).toJson());
        }
        return sb.append("]").toString();
    }

    // ---------- small helper methods ----------
    private static String text(Map<String, String> m, String key) {
        String v = m.get(key);
        return v == null ? "" : v;
    }
    private static double num(Map<String, String> m, String key) {
        try { return Double.parseDouble(m.getOrDefault(key, "").trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " must be a valid number."); }
    }
    private static int whole(Map<String, String> m, String key) {
        try { return Integer.parseInt(m.getOrDefault(key, "").trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " must be a whole number."); }
    }
    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    // Handles the browser's CORS pre-flight request (used when frontend and
    // backend run on different ports).
    private static boolean handleCors(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            addCors(ex);
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }
        return false;
    }
    private static void addCors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
    }
    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        addCors(ex);
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
    private static void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, "{\"error\":" + Json.str(message) + "}");
    }
    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css; charset=utf-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=utf-8";
        return "text/plain; charset=utf-8";
    }

    // The 12 initial sample gadgets (used only the first time, when no CSV data exists).
    private static void seedSampleGadgets(InventoryManager inv) {
        try {
            inv.addGadget(new MobileGadget("MOB001", "Galaxy S24", "Samsung", "256 GB storage", 71249.05, 74999, 10, 12));
            inv.addGadget(new MobileGadget("MOB002", "iPhone 15", "Apple", "128 GB storage", 75905, 79900, 5, 12));
            inv.addGadget(new MobileGadget("MOB003", "Redmi Note 13", "Xiaomi", "128 GB storage", 17099.05, 17999, 20, 12));
            inv.addGadget(new MobileGadget("MOB004", "Pixel 8", "Google", "128 GB storage", 66499.05, 69999, 0, 12));
            inv.addGadget(new LaptopGadget("LAP001", "Inspiron 15", "Dell", "16 GB RAM", 49491, 54990, 8, 24));
            inv.addGadget(new LaptopGadget("LAP002", "MacBook Air M2", "Apple", "8 GB RAM", 89910, 99900, 4, 24));
            inv.addGadget(new LaptopGadget("LAP003", "IdeaPad Slim 3", "Lenovo", "8 GB RAM", 38691, 42990, 12, 24));
            inv.addGadget(new LaptopGadget("LAP004", "Lenovo IdeaPad", "Lenovo", "16 GB RAM", 51300, 57000, 20, 24));
            inv.addGadget(new SmartwatchGadget("WAT001", "Apple Watch SE", "Apple", "40 mm display", 24900, 27900, 6, 12));
            inv.addGadget(new SmartwatchGadget("WAT002", "Samsung Galaxy Watch", "Samsung", "44 mm display", 19999, 22999, 8, 12));
            inv.addGadget(new SmartwatchGadget("WAT003", "Redmi Watch 5", "Xiaomi", "47 mm display", 4999, 6999, 12, 12));
            inv.addGadget(new SmartwatchGadget("WAT004", "Noise ColorFit", "Noise", "1.85-inch display", 2499, 4999, 0, 12));
        } catch (DuplicateSerialException e) {
            System.out.println("Seed error: " + e.getMessage());
        }
    }
}
