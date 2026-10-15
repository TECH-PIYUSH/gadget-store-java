// ============================================================================
// DuplicateSerialException.java
// CUSTOM (user-defined) checked exception.
// Thrown when someone tries to add a gadget whose serial number already exists.
// ============================================================================
public class DuplicateSerialException extends Exception {
    public DuplicateSerialException(String message) {
        super(message);
    }
}
