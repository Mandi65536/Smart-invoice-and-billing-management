
package org.example;

public class Customer {

    private int customerId;
    private String customerName;
    private String phone;
    private String email;
    private String billingAddress;

    public Customer(int customerId, String customerName,
                    String phone, String email,
                    String billingAddress) {

        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.billingAddress = billingAddress;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getBillingAddress() {
        return billingAddress;
    }
}
