// ============================================================================
// Gadget.java
// Abstract PARENT class for every gadget sold in the store.
//
// Demonstrates two OOP pillars:
//   * ABSTRACTION   -> this is an abstract class with an abstract method.
//   * ENCAPSULATION -> all fields are private and reached only through getters
//                      and controlled methods (stock can never become negative).
// ============================================================================
public abstract class Gadget {

    // ENCAPSULATION: every field is private so the outside world cannot change
    // them directly. They are read through getters below.
    private final String serialNumber;  // 'final' -> a serial number never changes once set
    private String name;
    private String brand;
    private double sellingPrice;
    private double originalPrice;
    private int stock;
    private int warrantyMonths;

    // CONSTRUCTOR: called by every subclass through super(...).
    public Gadget(String serialNumber, String name, String brand,
                  double sellingPrice, double originalPrice, int stock, int warrantyMonths) {
        this.serialNumber = serialNumber;
        this.name = name;
        this.brand = brand;
        this.sellingPrice = sellingPrice;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.warrantyMonths = warrantyMonths;
    }

    // ABSTRACTION + POLYMORPHISM:
    // Every subclass MUST provide its own version of these two methods.
    public abstract String displaySpecifications(); // e.g. "256 GB storage"
    public abstract String getCategory();           // e.g. "Mobile"

    // --- Getters: read-only access to the private data ---
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public double getSellingPrice() { return sellingPrice; }
    public double getOriginalPrice() { return originalPrice; }
    public int getStock() { return stock; }
    public int getWarrantyMonths() { return warrantyMonths; }

    // Discount percentage, rounded to a whole number (0 when there is no discount).
    public int getDiscountPercent() {
        if (originalPrice <= 0 || sellingPrice >= originalPrice) return 0;
        return (int) Math.round((originalPrice - sellingPrice) / originalPrice * 100);
    }

    public boolean isInStock() { return stock > 0; }

    // CONTROLLED stock change. This is the ONLY way to reduce stock, and it
    // guarantees stock can never go below zero (an encapsulation rule).
    public void reduceStock(int quantity) throws OutOfStockException {
        if (quantity <= 0) {
            throw new OutOfStockException("Quantity must be a positive whole number.");
        }
        if (quantity > stock) {
            throw new OutOfStockException("Only " + stock + " unit(s) of " + name + " left in stock.");
        }
        this.stock = this.stock - quantity;
    }

    // Converts this gadget into a JSON object string for the frontend.
    // It calls getCategory() and displaySpecifications(), which are provided by
    // the subclass -> this is runtime POLYMORPHISM in action.
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"serialNumber\":").append(Json.str(serialNumber)).append(",");
        sb.append("\"name\":").append(Json.str(name)).append(",");
        sb.append("\"brand\":").append(Json.str(brand)).append(",");
        sb.append("\"category\":").append(Json.str(getCategory())).append(",");
        sb.append("\"specifications\":").append(Json.str(displaySpecifications())).append(",");
        sb.append("\"sellingPrice\":").append(sellingPrice).append(",");
        sb.append("\"originalPrice\":").append(originalPrice).append(",");
        sb.append("\"discountPercent\":").append(getDiscountPercent()).append(",");
        sb.append("\"stock\":").append(stock).append(",");
        sb.append("\"warrantyMonths\":").append(warrantyMonths);
        sb.append("}");
        return sb.toString();
    }
}
