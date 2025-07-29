package tax_generator.tax_generator.service;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tax_generator.tax_generator.model.Item;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleOcrService {

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String  credentialsPath;

    public List<Item> extractItemsFromFiles(List<MultipartFile> files) throws IOException {
        List<Item> items = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return items; 
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
            for (MultipartFile file : files) {
                ByteString imgBytes = ByteString.copyFrom(file.getBytes());

                Image img = Image.newBuilder().setContent(imgBytes).build();
                Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                AnnotateImageResponse response = vision.batchAnnotateImages(List.of(request)).getResponses(0);
                if (response.hasError()) {
                    System.out.println("Error: " + response.getError().getMessage());
                    continue;
                }

                String ocrText = response.getFullTextAnnotation().getText();
                items.addAll(parseItemsFromText(ocrText));
            }
        }

        return items;
    }

    private List<Item> parseItemsFromText(String text) {
        List<Item> items = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?i)(.*?)(\\d+)\\s+(\\d+\\.\\d{2})$");
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                try {
                    String name = matcher.group(1).trim();
                    int qty = Integer.parseInt(matcher.group(2));
                    double price = Double.parseDouble(matcher.group(3));
                    items.add(new Item(name, qty, price));
                } catch (Exception ignored) {}
            }
        }

        return items;
    }
}