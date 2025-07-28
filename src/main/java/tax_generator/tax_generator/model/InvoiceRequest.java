package tax_generator.tax_generator.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class InvoiceRequest {
    private String location;
    private List<Item> items;
    private List<MultipartFile> files;

    public InvoiceRequest(String location, List<Item> items, List<MultipartFile> files) {
        this.location = location;
        this.items = items;
        this.files = files;
    }

}
