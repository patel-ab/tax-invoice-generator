package tax_generator.tax_generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Item {
    private String itemName;
    private int quantity;
    private double amount;
}