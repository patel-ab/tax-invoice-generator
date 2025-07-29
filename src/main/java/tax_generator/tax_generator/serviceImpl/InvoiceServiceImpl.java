package tax_generator.tax_generator.serviceImpl;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import tax_generator.tax_generator.model.InvoiceRequest;
import tax_generator.tax_generator.model.Item;
import tax_generator.tax_generator.service.InvoiceService;
import tax_generator.tax_generator.util.OcrUtil;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Map<String, Double> TAX_RATES = new HashMap<>();

    static {
        TAX_RATES.put("MI", 0.06);
        TAX_RATES.put("CA", 0.075);
        TAX_RATES.put("NY", 0.04);
    }

    private double getTaxRate(String location) {
        return TAX_RATES.getOrDefault(location.toUpperCase(), 0.05);
    }

    @Override
    public byte[] generateInvoicePdf(InvoiceRequest request) {
        mergeOcrItemsIfPresent(request);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            DecimalFormat df = new DecimalFormat("0.00");

            addHeader(doc);
            addInvoiceInfo(doc, request);
            double subtotal = addItemsTable(doc, request.getItems(), df);
            addTotalsSection(doc, subtotal, request.getLocation(), df);
            addFooter(doc);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void mergeOcrItemsIfPresent(InvoiceRequest request) {
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            try {
                List<Item> extractedItems = OcrUtil.extractItemsFromFiles(request.getFiles());
                for (Item i : extractedItems) {
                    System.out.println("OCR Item -> Name: " + i.getItemName() + ", Qty: " + i.getQuantity() + ", Amt: " + i.getAmount());
                }
                request.getItems().addAll(extractedItems);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addHeader(Document doc) {
        Paragraph header = new Paragraph("INVOICE")
                .setFontSize(24)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(10);
        doc.add(header);
        doc.add(new Paragraph("\n"));
    }

    private void addInvoiceInfo(Document doc, InvoiceRequest request) {
        doc.add(new Paragraph("Invoice No: " + UUID.randomUUID().toString().substring(0, 8)));
        doc.add(new Paragraph("Date: " + LocalDate.now()));
        doc.add(new Paragraph("Location: " + request.getLocation().toUpperCase()));
        doc.add(new Paragraph("\n"));
    }

    private double addItemsTable(Document doc, List<Item> items, DecimalFormat df) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2}))
                .useAllAvailableWidth();

        String[] headers = {"Item", "Quantity", "Unit Price", "Total"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
        }

        double subtotal = 0;
        boolean alternate = false;
        for (Item item : items) {
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
        return subtotal;
    }

    private void addTotalsSection(Document doc, double subtotal, String location, DecimalFormat df) {
        double taxRate = getTaxRate(location);
        double tax = subtotal * taxRate;
        double total = subtotal + tax;

        doc.add(new LineSeparator(new SolidLine()));
        doc.add(new Paragraph("Subtotal: $" + df.format(subtotal)).setTextAlignment(TextAlignment.RIGHT));
        doc.add(new Paragraph("Tax (" + (taxRate * 100) + "%): $" + df.format(tax)).setTextAlignment(TextAlignment.RIGHT));
        doc.add(new Paragraph("Total: $" + df.format(total)).setTextAlignment(TextAlignment.RIGHT).setBold());
        doc.add(new LineSeparator(new SolidLine()));
        doc.add(new Paragraph("\n"));
    }

    private void addFooter(Document doc) {
        doc.add(new Paragraph("Thank you for your business!")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setItalic());
    }
}
