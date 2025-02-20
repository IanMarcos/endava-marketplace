package com.endava.marketplace.backend.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import jakarta.annotation.PostConstruct;
import org.apache.tomcat.util.http.fileupload.impl.InvalidContentTypeException;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;

@Component
public class StorageClient {
    @Value("${spring.cloud.azure.storage.account-name}")
    private String storageAccountName;

    @Value("${spring.cloud.azure.storage.account-key}")
    private String storageAccountKey;

    @Value("${spring.cloud.azure.storage.endpoint}")
    private String storageEndpoint;

    private BlobContainerClient imagesClient;
    private BlobContainerClient thumbsClient;

    private final Set<String> VALID_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    @PostConstruct
    public void postConstruct() {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(storageAccountName, storageAccountKey);
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .endpoint(storageEndpoint)
                .credential(credential)
                .buildClient();
        imagesClient = serviceClient.getBlobContainerClient("images");
        thumbsClient = serviceClient.getBlobContainerClient("thumbs");
    }

    public void uploadImages(List<MultipartFile> images, Long listingId) throws IOException {
        int count  = fetchImagesURLS(listingId).size() + 1;

        for (MultipartFile image : images) {
            String contentType = image.getContentType();

            assert contentType != null;
            String extension = contentType.split("/")[1];

            if (!VALID_EXTENSIONS.contains(extension)) {
                throw new InvalidContentTypeException("Image #" + count + " doesn't have a valid extension");
            }

            InputStream data = image.getInputStream();

            BlobHttpHeaders imageHeaders = new BlobHttpHeaders()
                    .setContentType(contentType)
                    .setContentDisposition("inline");

            BlobClient blob = imagesClient.getBlobClient(String.format("%d_%d", listingId, count));

            blob.upload(data, image.getSize(), true);
            blob.setHttpHeaders(imageHeaders);

            if (count == 1) {
                data = createThumbnail(image, extension);

                BlobClient thumbBlob = thumbsClient.getBlobClient(String.format("%d_thumb", listingId));

                thumbBlob.upload(data, true);
                thumbBlob.setHttpHeaders(imageHeaders);
            }

            count++;
        }
    }

    public List<String> fetchImagesURLS(Long id) {
        List<String> imagesEndpoints = new ArrayList<>();
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions().setPrefix(id.toString() + "_");
        imagesClient.listBlobs(listBlobsOptions, Duration.ofSeconds(5)).forEach(blobItem ->
                imagesEndpoints.add(imagesClient.getBlobClient(blobItem.getName()).getBlobUrl()));
        return imagesEndpoints;
    }

    public String fetchThumbnailURL(Long id) {
        return thumbsClient.getBlobClient(String.format("%d_thumb", id)).exists() ? thumbsClient.getBlobClient(String.format("%d_thumb", id)).getBlobUrl() : null;
    }

    private InputStream createThumbnail(MultipartFile image, String extension) throws IOException {
        ByteArrayOutputStream thumbOutput = new ByteArrayOutputStream();
        BufferedImage originalImage = ImageIO.read(image.getInputStream());
        BufferedImage thumbImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, 400);
        ImageIO.write(thumbImage, extension, thumbOutput);
        return new ByteArrayInputStream(thumbOutput.toByteArray());
    }
}
