// ============================================================================
// Customer.java
// Simple data class that stores one customer's details.
// ENCAPSULATION: private fields + getters.
// ============================================================================
public class Customer {

    private String fullName;
    private String mobile;
    private String email;     // optional
    private String address;
    private String city;
    private String pinCode;

    public Customer(String fullName, String mobile, String email,
                    String address, String city, String pinCode) {
        this.fullName = fullName;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.city = city;
        this.pinCode = pinCode;
    }

    public String getFullName() { return fullName; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getPinCode() { return pinCode; }
}
