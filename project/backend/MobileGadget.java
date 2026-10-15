// ============================================================================
// MobileGadget.java
// INHERITANCE : MobileGadget "is-a" Gadget  (extends Gadget).
// POLYMORPHISM: it overrides displaySpecifications() and getCategory().
// ============================================================================
public class MobileGadget extends Gadget {

    // Field specific to mobiles, e.g. "256 GB storage".
    private String storage;

    public MobileGadget(String serialNumber, String name, String brand, String storage,
                        double sellingPrice, double originalPrice, int stock, int warrantyMonths) {
        // Pass the common details up to the Gadget constructor.
        super(serialNumber, name, brand, sellingPrice, originalPrice, stock, warrantyMonths);
        this.storage = storage;
    }

    public String getStorage() { return storage; }

    @Override
    public String getCategory() { return "Mobile"; }

    @Override
    public String displaySpecifications() {
        return storage; // a mobile is described by its storage
    }
}
