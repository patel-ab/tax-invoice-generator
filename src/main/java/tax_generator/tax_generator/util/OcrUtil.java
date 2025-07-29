package tax_generator.tax_generator.util;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.multipart.MultipartFile;
import tax_generator.tax_generator.model.Item;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrUtil {

    private static final Tesseract tesseract = new Tesseract();

    static {

        tesseract.setDatapath("Add Path here to tess data"); // Add Path here to tessdata
        tesseract.setLanguage("eng");
    }

    public static List<String> extractTextFromFiles(List<MultipartFile> files) throws IOException, TesseractException {
        List<String> extractedTexts = new ArrayList<>();

        for (MultipartFile file : files) {
            File temp = File.createTempFile("ocr-", file.getOriginalFilename());
            file.transferTo(temp);
            String result = tesseract.doOCR(temp);
            extractedTexts.add(result);
            temp.delete();
        }

        return extractedTexts;
    }


    public static List<Item> extractItemsFromFiles(List<MultipartFile> files) throws IOException, TesseractException {
        List<Item> items = new ArrayList<>();

        for (MultipartFile file : files) {
            File temp = File.createTempFile("ocr-", file.getOriginalFilename());
            file.transferTo(temp);
            String text = tesseract.doOCR(temp);
            temp.delete();

            String[] lines = text.split("\\r?\\n");
            Pattern totalPattern = Pattern.compile("(?i)^(.*?)(TOTAL|SUBTOTAL)[^\\d]*([\\d]+(\\.\\d{2})?)");

            boolean found = false;
            for (String line : lines) {
                Matcher matcher = totalPattern.matcher(line.trim());
                if (matcher.find()) {
                    try {
                        double amount = Double.parseDouble(matcher.group(3));
                        items.add(new Item(file.getOriginalFilename(), 1, amount));
                        found = true;
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (!found) {
                System.out.println("No TOTAL/SUBTOTAL found in: " + file.getOriginalFilename());
            }
        }

        return items;
    }
}