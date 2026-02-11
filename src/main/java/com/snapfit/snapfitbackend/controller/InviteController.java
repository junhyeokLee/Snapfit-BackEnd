package com.snapfit.snapfitbackend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
public class InviteController {

    @GetMapping("/invite")
    public ResponseEntity<String> inviteRedirect(@RequestParam String token) {
        String deeplink = "snapfit://invite?token=" + token;
        String html = """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>SnapFit Invite</title>
              <meta http-equiv="refresh" content="0; url=%s" />
            </head>
            <body>
              <p>SnapFit 앱으로 이동 중입니다.</p>
              <p><a href="%s">앱이 열리지 않으면 여기를 눌러주세요.</a></p>
            </body>
            </html>
            """.formatted(deeplink, deeplink);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping(value = "/invite/preview.png", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] invitePreviewImage() throws IOException {
        int width = 800;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color start = new Color(0x13, 0xC8, 0xEC);
        Color end = new Color(0x8B, 0x5C, 0xF6);
        GradientPaint paint = new GradientPaint(0, 0, start, width, height, end);
        g.setPaint(paint);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(0x12, 0x12, 0x12, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.drawString("SnapFit", 48, 120);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("Album Invite", 48, 160);

        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
