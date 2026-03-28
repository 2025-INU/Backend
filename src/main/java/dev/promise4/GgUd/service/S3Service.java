package dev.promise4.GgUd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * 프로필 이미지 업로드
     * @return S3 URL
     */
    public String uploadProfileImage(Long userId, MultipartFile file) {
        validateImageFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String key = "profile-images/" + userId + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
            log.info("Profile image uploaded: userId={}, key={}", userId, key);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드에 실패했습니다", e);
        }
    }

    /**
     * S3 객체 삭제 (기존 프로필 이미지 제거용)
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains(".amazonaws.com/")) return;

        String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("S3 image deleted: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to delete S3 image: key={}, error={}", key, e.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 파일 크기는 5MB 이하여야 합니다");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
