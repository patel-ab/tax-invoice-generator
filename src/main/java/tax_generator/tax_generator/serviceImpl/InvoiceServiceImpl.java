package tax_generator.tax_generator.serviceImpl;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.ILineDrawer;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import tax_generator.tax_generator.model.InvoiceRequest;
import tax_generator.tax_generator.model.Item;
import tax_generator.tax_generator.service.InvoiceService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;


@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Map<String, Double> TAX_RATES = new HashMap<>();

    static {
        TAX_RATES.put("MI", 0.06);
        TAX_RATES.put("CA", 0.075);
        TAX_RATES.put("NY", 0.04);
        // Default tax rate if unknown
    }


    @Override
    public byte[] generateInvoicePdf(InvoiceRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            DecimalFormat df = new DecimalFormat("0.00");

            // === Header ===
            Paragraph header = new Paragraph("INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBackgroundColor(ColorConstants.DARK_GRAY)
                    .setPadding(10);
            doc.add(header);

            doc.add(new Paragraph("\n"));

            // === Invoice Info ===
            doc.add(new Paragraph("Invoice No: " + UUID.randomUUID().toString().substring(0, 8)));
            doc.add(new Paragraph("Date: " + LocalDate.now()));
            doc.add(new Paragraph("Location: " + request.getLocation().toUpperCase()));
            doc.add(new Paragraph("\n"));

            // === Table Header ===
            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2}))
                    .useAllAvailableWidth();

            String[] headers = { "Item", "Quantity", "Unit Price", "Total" };
            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER));
            }

            double subtotal = 0;
            boolean alternate = false;
            for (Item item : request.getItems()) {
                double lineTotal = item.getAmount() * item.getQuantity();
                subtotal += lineTotal;

                Color rowColor = alternate ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY;
                alternate = !alternate;

                table.addCell(new Cell().add(new Paragraph(item.getItemName())).setBackgroundColor(rowColor));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity())))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(rowColor));
                table.addCell(new Cell().add(new Paragraph("$" + df.format(item.getAmount())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBackgroundColor(rowColor));
                table.addCell(new Cell().add(new Paragraph("$" + df.format(lineTotal)))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBackgroundColor(rowColor));
            }

            doc.add(table);

            doc.add(new Paragraph("\n"));

            // === Totals Section ===
            double taxRate = getTaxRate(request.getLocation());
            double tax = subtotal * taxRate;
            double total = subtotal + tax;

            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()));

            doc.add(new Paragraph("Subtotal: $" + df.format(subtotal)).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Tax (" + (taxRate * 100) + "%): $" + df.format(tax)).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Total: $" + df.format(total)).setTextAlignment(TextAlignment.RIGHT).setBold());

            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()));
            doc.add(new Paragraph("\n"));

            // === Footer ===
            doc.add(new Paragraph("Thank you for your business!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setItalic());

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private double getTaxRate(String location) {
        return switch (location.toUpperCase()) {
            case "MI" -> 0.06;
            case "CA" -> 0.075;
            case "NY" -> 0.04;
            default -> 0.05;
        };
    }
}
