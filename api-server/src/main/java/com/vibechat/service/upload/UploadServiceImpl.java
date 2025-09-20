package com.vibechat.service.upload;

import com.vibechat.domain.Message;
import com.vibechat.dto.UploadResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadServiceImpl.class);

    private final Path fileStorageLocation;
    private final Tika tika = new Tika();
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Value("${media.max-video-seconds}")
    private int maxVideoSeconds;

    private final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif", "video/mp4"
    );

    public UploadServiceImpl(@Value("${media.upload-dir}") String uploadDir,
                             @Value("${media.ffmpeg.path}") String ffmpegPath,
                             @Value("${media.ffprobe.path}") String ffprobePath) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.ffprobe = new FFprobe(ffprobePath);
    }

    @Override
    public UploadResponse storeFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is null or empty");
        }

        long maxBytes = parseFileSize(maxFileSize);
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds limit: " + maxFileSize);
        }

        String detectedMimeType;
        try {
            detectedMimeType = tika.detect(file.getInputStream(), originalFilename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to detect MIME type", e);
        }

        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new IllegalArgumentException("Unsupported file type: " + detectedMimeType);
        }

        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + fileExtension;
        if (storedFilename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence");
        }

        Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            if (detectedMimeType.startsWith("image/")) {
                return processImage(targetLocation, storedFilename, detectedMimeType);
            } else if (detectedMimeType.startsWith("video/")) {
                return processVideo(targetLocation, storedFilename);
            } else {
                throw new IllegalArgumentException("Unknown message type for MIME: " + detectedMimeType);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename, ex);
        }
    }

    private UploadResponse processImage(Path targetLocation, String storedFilename, String mimeType) throws IOException {
        Message.MessageType messageType = mimeType.equals("image/gif") ? Message.MessageType.GIF : Message.MessageType.IMAGE;
        String thumbFilename = UUID.randomUUID().toString() + ".jpg";
        Path thumbLocation = this.fileStorageLocation.resolve(thumbFilename);
        Thumbnails.of(targetLocation.toFile()).size(512, 512).toFile(thumbLocation.toFile());
        return UploadResponse.builder().type(messageType).url("/uploads/" + storedFilename).thumbUrl("/uploads/" + thumbFilename).build();
    }

    private UploadResponse processVideo(Path targetLocation, String storedFilename) throws IOException {
        try {
            FFmpegProbeResult probeResult = ffprobe.probe(targetLocation.toString());
            FFmpegFormat format = probeResult.getFormat();
            if (format.duration > maxVideoSeconds) {
                Files.delete(targetLocation);
                throw new IllegalArgumentException("Video duration exceeds limit: " + maxVideoSeconds + " seconds.");
            }
            short durationSec = (short) format.duration;
            String thumbFilename = UUID.randomUUID().toString() + ".jpg";
            Path thumbLocation = this.fileStorageLocation.resolve(thumbFilename);
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(targetLocation.toString())
                    .overrideOutputFiles(true)
                    .addOutput(thumbLocation.toString())
                    .setFormat("image2")
                    .setFrames(1)
                    .setVideoFilter("select='eq(n,0)',scale=512:-1")
                    .done();
            ffmpeg.run(builder);
            return UploadResponse.builder().type(Message.MessageType.VIDEO).url("/uploads/" + storedFilename).thumbUrl("/uploads/" + thumbFilename).durationSec(durationSec).build();
        } catch (Exception e) {
            try { Files.deleteIfExists(targetLocation); } catch (IOException ex) { logger.error("Failed to cleanup video file after error", ex); }
            throw new RuntimeException("Failed to process video file", e);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }

    private long parseFileSize(String fileSize) {
        fileSize = fileSize.toUpperCase();
        if (fileSize.endsWith("MB")) {
            return Long.parseLong(fileSize.substring(0, fileSize.length() - 2)) * 1024 * 1024;
        } else if (fileSize.endsWith("KB")) {
            return Long.parseLong(fileSize.substring(0, fileSize.length() - 2)) * 1024;
        } else if (fileSize.endsWith("B")) {
            return Long.parseLong(fileSize.substring(0, fileSize.length() - 1));
        } else {
            return Long.parseLong(fileSize);
        }
    }
}


