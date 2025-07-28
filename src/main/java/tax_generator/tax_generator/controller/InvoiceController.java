package tax_generator.tax_generator.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tax_generator.tax_generator.model.InvoiceRequest;
import tax_generator.tax_generator.model.Item;
import tax_generator.tax_generator.service.InvoiceService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    @Autowired
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }


    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CrossOrigin("*")
    public ResponseEntity<byte[]> generateInvoice(
            @RequestPart("location") String location,
            @RequestPart("items") MultipartFile itemsJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        List<Item> items;
        try (InputStream is = itemsJson.getInputStream()) {
            items = new ObjectMapper().readValue(is, new TypeReference<List<Item>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse items JSON", e);
        }

        InvoiceRequest request = new InvoiceRequest(location, items, files); // Create this constructor or set manually

        byte[] pdfBytes = invoiceService.generateInvoicePdf(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
