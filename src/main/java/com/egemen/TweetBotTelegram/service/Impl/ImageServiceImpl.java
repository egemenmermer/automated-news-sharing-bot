package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

@Service
public class ImageServiceImpl implements ImageService {

    @Override
    public void generateImageFromText(String text, OutputStream outputStream) throws Exception {
        // Şablon görselini yükle
        File templateFile = new ClassPathResource("static/template.png").getFile();
        BufferedImage templateImage = ImageIO.read(templateFile);

        // Yeni bir grafik objesi oluştur
        Graphics2D g2d = templateImage.createGraphics();

        // Yazı stilini ayarla
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.setColor(Color.WHITE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Yazıyı ortalamak için FontMetrics kullan
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int imageWidth = templateImage.getWidth();
        int imageHeight = templateImage.getHeight();

        // Satır kaydırma için kullanılacak parametreler
        int lineHeight = fontMetrics.getHeight();
        String[] words = text.split(" ");  // Kelimeleri boşluklardan ayır

        StringBuilder currentLine = new StringBuilder();
        int y = (imageHeight - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent(); // Dikeyde ortalama
        for (String word : words) {
            // Gelecek kelimeyi ekleyip satırın taşmasını kontrol et
            String testLine = currentLine.toString() + word + " ";
            if (fontMetrics.stringWidth(testLine) <= imageWidth) {
                currentLine.append(word).append(" ");
            } else {
                // Mevcut satır dolmuşsa, bu satırı yaz ve yeni satıra geç
                int x = (imageWidth - fontMetrics.stringWidth(currentLine.toString())) / 2; // Yatayda ortalama
                g2d.drawString(currentLine.toString(), x, y);
                currentLine = new StringBuilder(word + " ");  // Yeni satıra geç
                y += lineHeight;  // Y koordinatını bir satır aşağı kaydır
            }
        }

        // Kalan son satırı da çiz
        int x = (imageWidth - fontMetrics.stringWidth(currentLine.toString())) / 2; // Yatayda ortalama
        g2d.drawString(currentLine.toString(), x, y);

        // Grafiği kapat
        g2d.dispose();

        // PNG kalitesi için özel yazıcı ayarları
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) throw new RuntimeException("No PNG writer found");

        ImageWriter writer = writers.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        // ImageOutputStream oluştur
        ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(ios);

        // Görseli yaz
        IIOImage image = new IIOImage(templateImage, null, null);
        writer.write(null, image, writeParam);

        // Kaynakları temizle
        writer.dispose();
        ios.close();
    }


}