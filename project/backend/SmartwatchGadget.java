// ============================================================================
// SmartwatchGadget.java
// INHERITANCE : SmartwatchGadget "is-a" Gadget.
// POLYMORPHISM: overrides displaySpecifications() and getCategory().
// ============================================================================
public class SmartwatchGadget extends Gadget {

    // Field specific to smartwatches, e.g. "44 mm display".
    private String display;

    public SmartwatchGadget(String serialNumber, String name, String brand, String display,
                            double sellingPrice, double originalPrice, int stock, int warrantyMonths) {
        super(serialNumber, name, brand, sellingPrice, originalPrice, stock, warrantyMonths);
        this.display = display;
    }

    public String getDisplay() { return display; }

    @Override
    public String getCategory() { return "Smartwatch"; }

    @Override
    public String displaySpecifications() {
        return display; // a smartwatch is described by its display
    }
}
