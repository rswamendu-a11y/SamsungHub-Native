package com.example.enterprisehub;

public class SaleItem {
    private int id;
    private String brand;
    private String model;
    private String variant;
    private String itemName; // Can keep as a composite or just display name
    private int quantity;
    private double price;
    private String segment;
    private long timestamp;

    public SaleItem(int id, String brand, String model, String variant, int quantity, double price, String segment, long timestamp) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.variant = variant;
        this.itemName = brand + " " + model + " " + variant;
        this.quantity = quantity;
        this.price = price;
        this.segment = segment;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getVariant() { return variant; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getSegment() { return segment; }
    public long getTimestamp() { return timestamp; }
}
