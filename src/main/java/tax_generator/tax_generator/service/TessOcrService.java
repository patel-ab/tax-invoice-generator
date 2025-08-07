package tax_generator.tax_generator.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tax_generator.tax_generator.model.Item;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TessOcrService {

    private final Tesseract tesseract;

    public TessOcrService(
            @Value("${TESS_DATA_PATH}") String tessDataPath,
            @Value("${TESS_LANGUAGE}") String tessLanguage) {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(tessLanguage);
    }

    public List<Item> extractItemsFromFiles(List<MultipartFile> files) throws IOException, TesseractException {
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
