import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ============================================================================
// StoreService.java
// The "brain" of the store. It VALIDATES input, processes orders and
// coordinates the InventoryManager and the FileManager.
//
// GENERIC COLLECTION used here:
//   * List<Order> -> the history of all successful orders.
// ============================================================================
public class StoreService {

    private final InventoryManager inventory;
    private final FileManager fileManager;
    private final List<Order> orderHistory = new ArrayList<>();

    private int nextOrderNumber = 1001; // used to build order IDs like "ORD-1001"

    // 'static final' constant: the payment method never changes in this project.
    public static final String PAYMENT_METHOD = "Cash on Delivery";

    public StoreService(InventoryManager inventory, FileManager fileManager) {
        this.inventory = inventory;
        this.fileManager = fileManager;
    }

    public InventoryManager getInventory() { return inventory; }
    public List<Order> getOrderHistory() { return orderHistory; }

    // On startup: load any saved orders and continue the order numbering.
    public void loadHistoryFromFile() {
        List<Order> saved = fileManager.loadOrders();
        orderHistory.addAll(saved);
        for (Order o : saved) {
            try {
                int num = Integer.parseInt(o.getOrderId().replace("ORD-", ""));
                if (num >= nextOrderNumber) nextOrderNumber = num + 1;
            } catch (NumberFormatException ignored) { }
        }
    }

    // ---- Add a new gadget (used by POST /api/gadgets) ----
    public Gadget addGadget(String category, String serial, String name, String brand, String specifications,
                            double sellingPrice, double originalPrice, int stock, int warrantyMonths)
            throws DuplicateSerialException {

        // Validation of all required fields and values.
        if (isBlank(category) || isBlank(serial) || isBlank(name) || isBlank(brand) || isBlank(specifications)) {
            throw new IllegalArgumentException("All fields are required.");
        }
        if (sellingPrice < 0 || originalPrice < 0) {
            throw new IllegalArgumentException("Prices cannot be negative.");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        if (warrantyMonths < 0) {
            throw new IllegalArgumentException("Warranty months cannot be negative.");
        }

        Gadget gadget = createGadget(category, serial, name, brand, specifications,
                                     sellingPrice, originalPrice, stock, warrantyMonths);

        inventory.addGadget(gadget);                     // may throw DuplicateSerialException
        fileManager.saveGadgets(inventory.getAllGadgets()); // FILE HANDLING: save new inventory
        return gadget;
    }

    // Builds the correct Gadget SUBCLASS based on the category (polymorphism).
    private Gadget createGadget(String category, String serial, String name, String brand, String specifications,
                                double sellingPrice, double originalPrice, int stock, int warrantyMonths) {
        switch (category.trim().toLowerCase()) {
            case "mobile":     return new MobileGadget(serial, name, brand, specifications, sellingPrice, originalPrice, stock, warrantyMonths);
            case "laptop":     return new LaptopGadget(serial, name, brand, specifications, sellingPrice, originalPrice, stock, warrantyMonths);
            case "smartwatch": return new SmartwatchGadget(serial, name, brand, specifications, sellingPrice, originalPrice, stock, warrantyMonths);
            default: throw new IllegalArgumentException("Category must be Mobile, Laptop or Smartwatch.");
        }
    }

    // ---- Place a Cash-on-Delivery order (used by POST /api/orders) ----
    public Order placeOrder(Customer customer, String serial, int quantity) throws OutOfStockException {

        // 1) Validate customer details.
        validateCustomer(customer);

        // 2) Validate quantity.
        if (quantity <= 0) {
            throw new OutOfStockException("Quantity must be a positive whole number.");
        }

        // 3) Find the gadget in the BACKEND (never trust the browser).
        Gadget gadget = inventory.getBySerial(serial);
        if (gadget == null) {
            throw new IllegalArgumentException("Selected gadget could not be found.");
        }
        if (!gadget.isInStock()) {
            throw new OutOfStockException(gadget.getName() + " is out of stock.");
        }

        // 4) Reduce stock. This re-checks quantity vs available stock and throws
        //    OutOfStockException if not enough. Stock changes ONLY after this succeeds.
        gadget.reduceStock(quantity);

        // 5) Price and total come from the BACKEND gadget, not from the request.
        double unitPrice = gadget.getSellingPrice();
        double total = unitPrice * quantity;
        String orderId = "ORD-" + (nextOrderNumber++);
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        Order order = new Order(orderId, customer, gadget.getSerialNumber(), gadget.getName(),
                                quantity, unitPrice, total, PAYMENT_METHOD, "Order Placed", dateTime);
        orderHistory.add(order);

        // 6) Persist everything AFTER the order succeeds. A failed order never
        //    reaches this point, so it never changes stock or the files.
        fileManager.saveGadgets(inventory.getAllGadgets()); // updated stock
        fileManager.appendCustomer(customer);               // append customer
        fileManager.appendOrder(order);                     // append order
        return order;
    }

    private void validateCustomer(Customer c) {
        if (c == null || isBlank(c.getFullName()) || isBlank(c.getMobile())
                || isBlank(c.getAddress()) || isBlank(c.getCity()) || isBlank(c.getPinCode())) {
            throw new IllegalArgumentException("Please fill in all required customer details.");
        }
        if (!c.getMobile().matches("[0-9]{10}")) {
            throw new IllegalArgumentException("Mobile number must be exactly 10 digits.");
        }
        if (!c.getPinCode().matches("[0-9]{6}")) {
            throw new IllegalArgumentException("PIN code must be exactly 6 digits.");
        }
        if (!isBlank(c.getEmail()) && !c.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email address is not valid.");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
