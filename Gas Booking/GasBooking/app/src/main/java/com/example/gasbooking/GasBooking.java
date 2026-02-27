package com.example.gasbooking;

public class GasBooking {
    private String date;
    private String time;
    private String quantity;
    private String deliveryDate; // Existing field for delivery date
    private String deliveryStatus; // Existing field for delivery status
    private String address; // New field for address

    public GasBooking(String date, String time, String quantity, String deliveryDate, String deliveryStatus, String address) {
        this.date = date;
        this.time = time;
        this.quantity = quantity;
        this.deliveryDate = deliveryDate; // Initialize delivery date
        this.deliveryStatus = deliveryStatus; // Initialize delivery status
        this.address = address; // Initialize address
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getDeliveryDate() { // Getter for delivery date
        return deliveryDate;
    }

    public String getDeliveryStatus() { // Getter for delivery status
        return deliveryStatus;
    }

    public String getAddress() { // Getter for address
        return address;
    }
}