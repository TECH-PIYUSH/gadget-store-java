// ============================================================================
// Order.java
// Stores one successful order.
//
// OBJECT RELATIONSHIP: an Order "has-a" Customer (composition). It also keeps a
// snapshot of the purchased gadget (serial, name and the price at buying time)
// plus the quantity, total amount and the fixed COD payment method.
// ============================================================================
public class Order {

    private String orderId;
    private Customer customer;     // has-a Customer
    private String gadgetSerial;
    private String gadgetName;
    private int quantity;
    private double unitPrice;      // price confirmed by the backend at order time
    private double totalAmount;
    private String paymentMethod;  // always "Cash on Delivery"
    private String status;         // e.g. "Order Placed"
    private String orderDateTime;

    public Order(String orderId, Customer customer, String gadgetSerial, String gadgetName,
                 int quantity, double unitPrice, double totalAmount,
                 String paymentMethod, String status, String orderDateTime) {
        this.orderId = orderId;
        this.customer = customer;
        this.gadgetSerial = gadgetSerial;
        this.gadgetName = gadgetName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.orderDateTime = orderDateTime;
    }

    public String getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public String getGadgetSerial() { return gadgetSerial; }
    public String getGadgetName() { return gadgetName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotalAmount() { return totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public String getOrderDateTime() { return orderDateTime; }

    // Converts this order into a JSON object string for the History table.
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"orderId\":").append(Json.str(orderId)).append(",");
        sb.append("\"customerName\":").append(Json.str(customer.getFullName())).append(",");
        sb.append("\"mobile\":").append(Json.str(customer.getMobile())).append(",");
        sb.append("\"email\":").append(Json.str(customer.getEmail())).append(",");
        sb.append("\"address\":").append(Json.str(customer.getAddress())).append(",");
        sb.append("\"city\":").append(Json.str(customer.getCity())).append(",");
        sb.append("\"pinCode\":").append(Json.str(customer.getPinCode())).append(",");
        sb.append("\"gadgetSerial\":").append(Json.str(gadgetSerial)).append(",");
        sb.append("\"gadgetName\":").append(Json.str(gadgetName)).append(",");
        sb.append("\"quantity\":").append(quantity).append(",");
        sb.append("\"unitPrice\":").append(unitPrice).append(",");
        sb.append("\"totalAmount\":").append(totalAmount).append(",");
        sb.append("\"paymentMethod\":").append(Json.str(paymentMethod)).append(",");
        sb.append("\"status\":").append(Json.str(status)).append(",");
        sb.append("\"orderDateTime\":").append(Json.str(orderDateTime));
        sb.append("}");
        return sb.toString();
    }
}
