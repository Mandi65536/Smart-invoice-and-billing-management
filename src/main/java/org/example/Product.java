
package org.example;

public class Product {

    private int productId;
    private String sku;
    private String productName;
    private String category;
    private double purchasePrice;
    private double sellingPrice;
    private int currentStock;
    private int reorderLevel;


    public Product(int productId, String sku, String productName,
                   String category, double purchasePrice,
                   double sellingPrice, int currentStock,
                   int reorderLevel) {

        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.category = category;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.currentStock = currentStock;
        this.reorderLevel = reorderLevel;
    }

    public int getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }


}
