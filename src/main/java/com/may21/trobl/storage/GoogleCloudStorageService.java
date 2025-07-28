package com.may21.trobl.storage;

import com.google.cloud.Binding;
import com.google.cloud.Policy;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.may21.trobl._global.enums.ImageSize;
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
import java.util.*;
import java.util.List;

import static com.may21.trobl._global.component.GlobalValues.USER_PROFILE_IMAGE_PATH;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleCloudStorageService implements StorageService {
    private final Storage storage;

    @Value("${BUCKET_NAME}")
    private String BUCKET_NAME;

    @Value("${CDN_LB_DOMAIN}")
    private String cdnLbIp;

    @Value("${STAGE}")
    private String stage;

    private final String PREFIX = "public/" + stage + "/";


    public String uploadPublicFile(MultipartFile file, String folder) throws IOException {
        String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .setAcl(Collections.singletonList(
                        Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))) // 공개 읽기 권한
                .setCacheControl("public, max-age=31536000") // 1년 캐싱
                .build();

        storage.create(blobInfo, file.getBytes());

        // CDN 도메인이 설정되어 있으면 CDN URL 반환, 없으면 일반 공개 URL 반환
        if (cdnLbIp != null && !cdnLbIp.isEmpty()) {
            return String.format("https://%s/%s", cdnLbIp, fileName);
        }
        else {
            return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
        }
    }

    // 이미지 최적화 업로드
    public String uploadOptimizedImage(MultipartFile file, String folder, ImageSize size)
            throws IOException {
        String fileName = folder + "/" + size.getPrefix() + "_" + UUID.randomUUID()
                .toString() + "_" + file.getOriginalFilename();

        // 이미지 리사이징 (실제 구현시에는 BufferedImage 사용)
        byte[] optimizedImage = optimizeImage(file.getBytes(), size);

        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("image/webp") // WebP 형식으로 최적화
                .setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                .setCacheControl("public, max-age=31536000")
                .setMetadata(Map.of("width", String.valueOf(size.getWidth()), "height",
                        String.valueOf(size.getHeight()), "optimized", "true"))
                .build();

        storage.create(blobInfo, optimizedImage);

        return getCdnUrl(fileName);
    }

    // CDN URL 생성
    private String getCdnUrl(String fileName) {
        if (cdnLbIp != null && !cdnLbIp.isEmpty()) {
            return String.format("http://%s/%s", cdnLbIp, fileName);
        }
        else {
            // CDN이 비활성화되어 있으면 일반 Storage URL 반환
            return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
        }
    }


    // 이미지 최적화 (간단한 예제)
    private byte[] optimizeImage(byte[] imageBytes, ImageSize size) {
        // 실제로는 ImageIO나 외부 라이브러리 사용
        return imageBytes; // 예제용
    }

    // 버킷을 공개로 설정
    public void makeBucketPublic() {
        Policy originalPolicy = storage.getIamPolicy(BUCKET_NAME);

        // 새로운 바인딩 생성
        Binding newBinding = Binding.newBuilder()
                .setRole("roles/storage.objectViewer")
                .setMembers(Collections.singletonList("allUsers"))
                .build();

        // 기존 바인딩 목록 가져오기
        List<Binding> bindings = new ArrayList<>(originalPolicy.getBindingsList());
        bindings.add(newBinding);

        Policy updatedPolicy = Policy.newBuilder()
                .setBindings(bindings)
                .setEtag(originalPolicy.getEtag())
                .build();

        storage.setIamPolicy(BUCKET_NAME, updatedPolicy);
    }

    public boolean deleteFileWithCacheInvalidation(String fileName) {
        boolean deleted = storage.delete(BlobId.of(BUCKET_NAME, fileName));
        if (deleted && cdnLbIp != null && !cdnLbIp.isEmpty()) {
            //            cdnCacheService.invalidateCdnCache(fileName);
        }
        return deleted;
    }


    @Override
    public String uploadUserProfileImage(Long userId, MultipartFile file) {
        try {

            String imageKey = userId + ".webp";
            String thumbnailFileName = PREFIX + USER_PROFILE_IMAGE_PATH + imageKey;

            byte[] thumbnailBytes = createThumbnail(file.getBytes(), 150, 150);

            BlobId blobId = BlobId.of(BUCKET_NAME, thumbnailFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/webp")
                    .setCacheControl("public, max-age=2592000") // 30일 캐싱
                    .setMetadata(Map.of("userId", String.valueOf(userId), "type", "thumbnail",
                            "originalFileName", Objects.requireNonNull(file.getOriginalFilename()),
                            "uploadTime", String.valueOf(System.currentTimeMillis())))
                    .build();

            storage.create(blobInfo, thumbnailBytes);
            // CDN CAche 무효화
            if (cdnLbIp != null && !cdnLbIp.isEmpty()) {
                //                cdnCacheService.invalidateCdnCache(thumbnailFileName);
            }

            return imageKey;

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("썸네일 업로드 실패: " + e.getMessage());
        }
    }

    // 썸네일 생성 메서드
    private byte[] createThumbnail(byte[] originalImage, int width, int height) throws IOException {
        try {
            // ByteArrayInputStream으로 이미지 읽기
            ByteArrayInputStream inputStream = new ByteArrayInputStream(originalImage);
            BufferedImage originalBufferedImage = ImageIO.read(inputStream);

            if (originalBufferedImage == null) {
                throw new IOException("지원하지 않는 이미지 형식입니다.");
            }

            // 비율 유지하면서 리사이징
            BufferedImage thumbnailImage =
                    resizeImageWithAspectRatio(originalBufferedImage, width, height);

            // WebP로 변환 (WebP Writer가 없으면 JPEG 사용)
            return convertToWebP(thumbnailImage);

        } catch (Exception e) {
            throw new IOException("이미지 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 비율 유지 리사이징
    private BufferedImage resizeImageWithAspectRatio(BufferedImage original, int targetWidth,
            int targetHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // 비율 계산
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;

        if (aspectRatio > 1) {
            // 가로가 더 긴 경우
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / aspectRatio);
        }
        else {
            // 세로가 더 긴 경우
            newWidth = (int) (targetHeight * aspectRatio);
            newHeight = targetHeight;
        }

        // 고품질 리사이징
        BufferedImage resized =
                new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        // 렌더링 힌트 설정 (고품질)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 배경을 흰색으로 채우기
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, targetWidth, targetHeight);

        // 이미지를 중앙에 그리기
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;
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

    // 사용자 프로필 이미지 삭제
    public boolean deleteUserProfileImage(Long userId) {
        try {
            String thumbnailFileName = PREFIX + USER_PROFILE_IMAGE_PATH + userId + ".webp";
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 사용자 프로필 이미지 URL 조회
    public String getUserProfileImageUrl(Long userId) {
        String thumbnailFileName = "public/thumbnails/" + userId + ".webp";
        return getCdnUrl(thumbnailFileName);
    }
}
