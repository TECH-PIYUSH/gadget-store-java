import java.io.*;
import java.util.*;

// ============================================================================
// FileManager.java
// ALL file handling lives here (the feature your professor requires).
//
// It reads and writes three CSV files inside a "data" folder:
//   * gadgets.csv   -> every gadget (serial, prices, stock, warranty, ...)
//   * customers.csv -> customers who placed an order
//   * orders.csv    -> successful COD orders
//
// Classes used: File, FileWriter, FileReader, BufferedReader (simple Java I/O).
// Every file is opened with try-with-resources so it is always closed safely.
// ============================================================================
public class FileManager {

    private final File dataDir       = new File("data");
    private final File gadgetsFile   = new File(dataDir, "gadgets.csv");
    private final File customersFile = new File(dataDir, "customers.csv");
    private final File ordersFile    = new File(dataDir, "orders.csv");

    private static final String GADGETS_HEADER =
        "category,serialNumber,name,brand,specifications,sellingPrice,originalPrice,stock,warrantyMonths";
    private static final String CUSTOMERS_HEADER =
        "fullName,mobile,email,address,city,pinCode";
    private static final String ORDERS_HEADER =
        "orderId,customerName,mobile,email,address,city,pinCode,gadgetSerial,gadgetName,quantity,unitPrice,totalAmount,paymentMethod,status,orderDateTime";

    // FILE-HANDLING METHOD: create the data folder and the 3 files if missing.
    public void ensureFiles() {
        try {
            if (!dataDir.exists()) dataDir.mkdirs();
            createWithHeaderIfMissing(gadgetsFile, GADGETS_HEADER);
            createWithHeaderIfMissing(customersFile, CUSTOMERS_HEADER);
            createWithHeaderIfMissing(ordersFile, ORDERS_HEADER);
        } catch (IOException e) {
            System.out.println("Could not create data files: " + e.getMessage());
        }
    }

    private void createWithHeaderIfMissing(File f, String header) throws IOException {
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) {   // try-with-resources -> auto close
                fw.write(header + System.lineSeparator());
            }
        }
    }

    // FILE-HANDLING METHOD: overwrite gadgets.csv with the current inventory.
    // Called after adding a gadget and after an order reduces the stock.
    public void saveGadgets(List<Gadget> gadgets) {
        try (FileWriter fw = new FileWriter(gadgetsFile)) {  // (no append) -> rewrite whole file
            fw.write(GADGETS_HEADER + System.lineSeparator());
            for (Gadget g : gadgets) {
                String line = String.join(",",
                    esc(g.getCategory()), esc(g.getSerialNumber()), esc(g.getName()), esc(g.getBrand()),
                    esc(g.displaySpecifications()), String.valueOf(g.getSellingPrice()),
                    String.valueOf(g.getOriginalPrice()), String.valueOf(g.getStock()),
                    String.valueOf(g.getWarrantyMonths()));
                fw.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            System.out.println("Could not save gadgets: " + e.getMessage());
        }
    }

    // FILE-HANDLING METHOD: read gadgets.csv back into Gadget objects on startup.
    public List<Gadget> loadGadgets() {
        List<Gadget> list = new ArrayList<>();
        if (!gadgetsFile.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(gadgetsFile))) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }   // skip the header row
                if (line.trim().isEmpty()) continue;
                List<String> f = parseCsvLine(line);
                if (f.size() < 9) continue;
                Gadget g = buildGadget(f.get(0), f.get(1), f.get(2), f.get(3), f.get(4),
                        Double.parseDouble(f.get(5)), Double.parseDouble(f.get(6)),
                        Integer.parseInt(f.get(7)), Integer.parseInt(f.get(8)));
                if (g != null) list.add(g);
            }
        } catch (IOException e) {
            System.out.println("Could not read gadgets: " + e.getMessage());
        }
        return list;
    }

    // Rebuilds the correct subclass from a saved category (polymorphism again).
    private Gadget buildGadget(String category, String serial, String name, String brand, String specs,
                               double sp, double op, int stock, int warranty) {
        switch (category.trim().toLowerCase()) {
            case "mobile":     return new MobileGadget(serial, name, brand, specs, sp, op, stock, warranty);
            case "laptop":     return new LaptopGadget(serial, name, brand, specs, sp, op, stock, warranty);
            case "smartwatch": return new SmartwatchGadget(serial, name, brand, specs, sp, op, stock, warranty);
            default:           return null;
        }
    }

    // FILE-HANDLING METHOD: APPEND a customer (existing rows are kept).
    public void appendCustomer(Customer c) {
        try (FileWriter fw = new FileWriter(customersFile, true)) {  // 'true' = append mode
            String line = String.join(",", esc(c.getFullName()), esc(c.getMobile()), esc(c.getEmail()),
                    esc(c.getAddress()), esc(c.getCity()), esc(c.getPinCode()));
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Could not save customer: " + e.getMessage());
        }
    }

    // FILE-HANDLING METHOD: APPEND an order (previous order history is kept intact).
    public void appendOrder(Order o) {
        try (FileWriter fw = new FileWriter(ordersFile, true)) {     // 'true' = append mode
            Customer c = o.getCustomer();
            String line = String.join(",", esc(o.getOrderId()), esc(c.getFullName()), esc(c.getMobile()),
                    esc(c.getEmail()), esc(c.getAddress()), esc(c.getCity()), esc(c.getPinCode()),
                    esc(o.getGadgetSerial()), esc(o.getGadgetName()), String.valueOf(o.getQuantity()),
                    String.valueOf(o.getUnitPrice()), String.valueOf(o.getTotalAmount()),
                    esc(o.getPaymentMethod()), esc(o.getStatus()), esc(o.getOrderDateTime()));
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Could not save order: " + e.getMessage());
        }
    }

    // FILE-HANDLING METHOD: read orders.csv back into Order objects on startup.
    public List<Order> loadOrders() {
        List<Order> list = new ArrayList<>();
        if (!ordersFile.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(ordersFile))) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                if (line.trim().isEmpty()) continue;
                List<String> f = parseCsvLine(line);
                if (f.size() < 15) continue;
                Customer c = new Customer(f.get(1), f.get(2), f.get(3), f.get(4), f.get(5), f.get(6));
                Order o = new Order(f.get(0), c, f.get(7), f.get(8),
                        Integer.parseInt(f.get(9)), Double.parseDouble(f.get(10)), Double.parseDouble(f.get(11)),
                        f.get(12), f.get(13), f.get(14));
                list.add(o);
            }
        } catch (IOException e) {
            System.out.println("Could not read orders: " + e.getMessage());
        }
        return list;
    }

    // --- Small CSV helpers that handle commas and quotes inside a field ---

    // Wraps a field in quotes (and doubles any inner quote) only when needed.
    private String esc(String field) {
        if (field == null) field = "";
        boolean needQuote = field.contains(",") || field.contains("\"")
                || field.contains("\n") || field.contains("\r");
        String v = field.replace("\"", "\"\"");
        return needQuote ? "\"" + v + "\"" : v;
    }

    // Splits one CSV line into fields, respecting quoted sections.
    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
                else cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
