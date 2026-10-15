// ============================================================================
// OutOfStockException.java
// CUSTOM (user-defined) checked exception.
// Thrown when a gadget is out of stock, when the requested quantity is more
// than the available stock, or when the quantity is not a positive number.
// ============================================================================
public class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message); // pass the readable message to the parent Exception class
    }
}
