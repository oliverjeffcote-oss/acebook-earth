package com.makersacademy.acebook.services;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Template s3Template;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${AWS_REGION}")  // Default fallback
    private String region;

    public String uploadImage(MultipartFile file) throws IOException {
        String key =  "images/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        s3Template.upload(bucket, key, file.getInputStream());

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    public void deleteImage(String key) {
        s3Template.deleteObject(bucket, key);
    }
}




