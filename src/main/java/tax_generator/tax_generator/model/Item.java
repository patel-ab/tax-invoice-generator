package tax_generator.tax_generator.model;

import lombok.Data;

@Data
public class Item {
    private String itemName;
    private int quantity;
    private double amount;
}