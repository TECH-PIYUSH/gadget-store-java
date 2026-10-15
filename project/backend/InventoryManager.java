import java.util.*;

// ============================================================================
// InventoryManager.java
// Manages the gadgets in the store and keeps serial numbers unique.
//
// GENERIC COLLECTIONS used here:
//   * Map<String, Gadget>  -> maps a unique serial number to its Gadget.
//                             (A LinkedHashMap keeps insertion order so the
//                             catalog shows gadgets in a tidy, predictable order.
//                             This is the "Dictionary<String,Gadget>" equivalent
//                             asked for in the assignment.)
//   * HashSet<String>      -> stores serial numbers to guarantee uniqueness.
// ============================================================================
public class InventoryManager {

    private final Map<String, Gadget> gadgets = new LinkedHashMap<>();
    private final HashSet<String> serialNumbers = new HashSet<>();

    // Adds a gadget. Throws our custom exception if the serial already exists.
    public void addGadget(Gadget gadget) throws DuplicateSerialException {
        if (serialNumbers.contains(gadget.getSerialNumber())) {
            throw new DuplicateSerialException(
                "A gadget with serial number '" + gadget.getSerialNumber() + "' already exists.");
        }
        gadgets.put(gadget.getSerialNumber(), gadget);
        serialNumbers.add(gadget.getSerialNumber()); // keep the set in sync with the map
    }

    public Gadget getBySerial(String serial) { return gadgets.get(serial); }

    public List<Gadget> getAllGadgets() { return new ArrayList<>(gadgets.values()); }

    public boolean hasSerial(String serial) { return serialNumbers.contains(serial); }

    public int size() { return gadgets.size(); }
}
