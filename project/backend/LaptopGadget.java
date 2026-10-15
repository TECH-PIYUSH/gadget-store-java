// ============================================================================
// LaptopGadget.java
// INHERITANCE : LaptopGadget "is-a" Gadget.
// POLYMORPHISM: overrides displaySpecifications() and getCategory().
// ============================================================================
public class LaptopGadget extends Gadget {

    // Field specific to laptops, e.g. "16 GB RAM".
    private String ram;

    public LaptopGadget(String serialNumber, String name, String brand, String ram,
                        double sellingPrice, double originalPrice, int stock, int warrantyMonths) {
        super(serialNumber, name, brand, sellingPrice, originalPrice, stock, warrantyMonths);
        this.ram = ram;
    }

    public String getRam() { return ram; }

    @Override
    public String getCategory() { return "Laptop"; }

    @Override
    public String displaySpecifications() {
        return ram; // a laptop is described by its RAM
    }
}
