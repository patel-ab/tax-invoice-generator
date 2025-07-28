package tax_generator.tax_generator.service;


import org.springframework.stereotype.Component;
import tax_generator.tax_generator.model.InvoiceRequest;

@Component
public interface InvoiceService {
    byte[] generateInvoicePdf(InvoiceRequest request);
}