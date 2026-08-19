package com.todayjikgwan.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.todayjikgwan.config.TodayJikgwanProperties;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이미지 저장 (REQ-F-203).
 *
 * <p><b>메타데이터 처리 (REQ-NF-007)</b><br>
 * 촬영 일시는 경기 자동 매칭에 필요하므로(REQ-F-204) 저장 전에 읽어둔다.
 * 그 다음 이미지를 디코딩 후 재인코딩하는데, 이 과정에서 EXIF 전체가 사라지므로
 * GPS 좌표를 포함한 위치 정보가 파일에 남지 않는다.
 *
 * <p><b>썸네일 (REQ-NF-002)</b><br>
 * 목록 화면이 원본을 내려받지 않도록 축소본을 함께 만든다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png");

    private final TodayJikgwanProperties properties;

    public record StoredImage(String originalUrl, String thumbnailUrl,
                              OffsetDateTime takenAt, int size, String mimeType) { }

    public boolean isAllowed(String contentType) {
        return contentType != null && ALLOWED.contains(contentType.toLowerCase());
    }

    public StoredImage store(byte[] bytes, String contentType) throws IOException {
        // 1. 저장 전에 촬영 일시만 추출한다
        OffsetDateTime takenAt = readTakenAt(bytes);

        // 2. 디코딩 → 재인코딩. 이 시점에 EXIF(위치 정보 포함)가 전부 제거된다
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            throw new IOException("이미지를 읽을 수 없습니다");
        }
        String ext = "image/png".equalsIgnoreCase(contentType) ? "png" : "jpg";
        String formatName = "png".equals(ext) ? "png" : "jpeg";

        Path dir = resolveDir();
        String name = UUID.randomUUID().toString().replace("-", "");
        Path originalPath = dir.resolve(name + "." + ext);
        Path thumbPath = dir.resolve(name + "_thumb." + ext);

        ImageIO.write(stripAlphaIfJpeg(source, formatName), formatName, originalPath.toFile());
        ImageIO.write(stripAlphaIfJpeg(resize(source), formatName), formatName, thumbPath.toFile());

        String base = properties.storage().publicBaseUrl() + "/" + relative(dir);
        return new StoredImage(base + originalPath.getFileName(),
                               base + thumbPath.getFileName(),
                               takenAt, (int) Files.size(originalPath), contentType);
    }

    /** REQ-F-204. EXIF DateTimeOriginal 이 없으면 null 을 반환하고 수동 선택으로 대체한다. */
    private OffsetDateTime readTakenAt(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (dir == null) {
                return null;
            }
            Date date = dir.getDateOriginal(java.util.TimeZone.getTimeZone("Asia/Seoul"));
            return date == null ? null
                    : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("Asia/Seoul"));
        } catch (Exception e) {
            log.debug("EXIF 판독 실패 (무시하고 진행): {}", e.getMessage());
            return null;
        }
    }

    private BufferedImage resize(BufferedImage src) {
        int max = properties.storage().thumbnailMaxPx();
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= max && h <= max) {
            return src;
        }
        double ratio = Math.min((double) max / w, (double) max / h);
        int nw = Math.max(1, (int) (w * ratio));
        int nh = Math.max(1, (int) (h * ratio));

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    /** JPEG 는 알파 채널을 지원하지 않아 흰 배경으로 합성한다. */
    private BufferedImage stripAlphaIfJpeg(BufferedImage src, String formatName) {
        if (!"jpeg".equals(formatName) || src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private Path resolveDir() throws IOException {
        java.time.LocalDate today = java.time.LocalDate.now();
        Path dir = Paths.get(properties.storage().baseDir())
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    private String relative(Path dir) {
        Path base = Paths.get(properties.storage().baseDir()).toAbsolutePath().normalize();
        return base.relativize(dir) + "/";
    }
}
