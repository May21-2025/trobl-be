package com.may21.trobl.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.utility.UrlMaker;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.may21.trobl._global.component.GlobalValues.USER_PROFILE_IMAGE_PATH;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleCloudStorageService implements StorageService {
    private final Storage storage;

    @Value("${BUCKET_NAME}")
    private String BUCKET_NAME;


    @Override
    public String uploadUserProfileImage(Long userId, MultipartFile file) {
        try {
            String imageKey = userId + ".webp";
            String thumbnailFileName =
                    GlobalValues.getPREFIX() + USER_PROFILE_IMAGE_PATH + imageKey;

            byte[] thumbnailBytes = createThumbnail(file.getBytes());

            BlobId blobId = BlobId.of(BUCKET_NAME, thumbnailFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/webp")
                    .setCacheControl("no-cache, max-age=0")
                    .setMetadata(Map.of("userId", String.valueOf(userId), "type", "thumbnail",
                            "originalFileName", Objects.requireNonNull(file.getOriginalFilename()),
                            "uploadTime", String.valueOf(System.currentTimeMillis())))
                    .build();

            storage.create(blobInfo, thumbnailBytes);
            return imageKey;

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("썸네일 업로드 실패: " + e.getMessage());
        }
    }


    @Override
    public List<String> uploadBannerImages(List<MultipartFile> adImages,
            AdvertisementDto.CreateAdvertisement advertisementRequest) {
        List<String> uploadedImageKeys = new ArrayList<>();
        AdvertisementDto.AdvertisementRequest adRequest =
                advertisementRequest.getAdvertisementRequest();
        List<AdvertisementDto.BannerRequest> bannerRequestList =
                advertisementRequest.getBannerRequestList();

        for (int i = 0; i < adImages.size(); i++) {
            MultipartFile file = adImages.get(i);
            AdvertisementDto.BannerRequest bannerRequest = bannerRequestList.get(i);
            try {
                String imageKey = UrlMaker.makeAdImageUrlKey(adRequest.getBrandName(),
                        bannerRequest.getType());
                String filePath = GlobalValues.getPREFIX() + "ads/" + imageKey;

                // 이미지 바이트 변환 (배너도 썸네일처럼 변환 필요하다면 createThumbnail 사용)
                byte[] imageBytes = file.getBytes();

                BlobId blobId = BlobId.of(BUCKET_NAME, filePath);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType("image/webp")
                        .setCacheControl("no-cache, max-age=0")
                        .setMetadata(Map.of(
                                "brandName", String.valueOf(adRequest.getBrandName()),
                                "url", String.valueOf(adRequest.getLinkUrl()),
                                "type", "banner",
                                "originalFileName", Objects.requireNonNull(file.getOriginalFilename()),
                                "uploadTime", String.valueOf(System.currentTimeMillis())
                        ))
                        .build();

                storage.create(blobInfo, imageBytes);
                uploadedImageKeys.add(imageKey);

            } catch (IOException e) {
                log.error("배너 업로드 실패: {}", e.getMessage());
                throw new RuntimeException("배너 업로드 실패: " + e.getMessage());
            }
        }

        return uploadedImageKeys;
    }


    // 썸네일 생성 메서드
    private byte[] createThumbnail(byte[] originalImage) throws IOException {
        try {
            // ByteArrayInputStream으로 이미지 읽기
            ByteArrayInputStream inputStream = new ByteArrayInputStream(originalImage);
            BufferedImage originalBufferedImage = ImageIO.read(inputStream);

            if (originalBufferedImage == null) {
                throw new IOException("지원하지 않는 이미지 형식입니다.");
            }

            // 비율 유지하면서 리사이징
            BufferedImage thumbnailImage = resizeImageWithAspectRatio(originalBufferedImage);

            // WebP로 변환 (WebP Writer가 없으면 JPEG 사용)
            return convertToWebP(thumbnailImage);

        } catch (Exception e) {
            throw new IOException("이미지 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 비율 유지 리사이징
    private BufferedImage resizeImageWithAspectRatio(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // 비율 계산
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;

        if (aspectRatio > 1) {
            newWidth = 150;
            newHeight = (int) (150 / aspectRatio);
        }
        else {
            // 세로가 더 긴 경우
            newWidth = (int) (150 * aspectRatio);
            newHeight = 150;
        }

        // 고품질 리사이징
        BufferedImage resized = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        // 렌더링 힌트 설정 (고품질)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 배경을 흰색으로 채우기
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 150, 150);

        // 이미지를 중앙에 그리기
        int x = (150 - newWidth) / 2;
        int y = (150 - newHeight) / 2;
        g2d.drawImage(original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), x, y,
                null);

        g2d.dispose();
        return resized;
    }

    // WebP 변환 (WebP 라이브러리가 없으면 JPEG 사용)
    private byte[] convertToWebP(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // WebP writer 시도
            ImageWriter webpWriter = ImageIO.getImageWritersByFormatName("webp")
                    .next();
            if (webpWriter != null) {
                ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
                webpWriter.setOutput(ios);
                webpWriter.write(image);
                webpWriter.dispose();
                ios.close();
            }
            else {
                throw new IOException("WebP writer not available");
            }
        } catch (Exception e) {
            // WebP 실패시 JPEG로 대체 (고품질)
            outputStream.reset();
            ImageIO.write(image, "jpg", outputStream);
        }

        return outputStream.toByteArray();
    }
}
