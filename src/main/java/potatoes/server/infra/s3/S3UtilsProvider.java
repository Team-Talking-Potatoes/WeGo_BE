package potatoes.server.infra.s3;

import static potatoes.server.utils.error.ErrorCode.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import potatoes.server.utils.error.exception.WeGoException;
import potatoes.server.utils.time.DateTimeUtils;

@RequiredArgsConstructor
@Component
public class S3UtilsProvider {
	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${spring.profiles.active:local}")
	private String profile;

	private static final String FOLDER_DELIMITER = "/";

	private final AmazonS3Client amazonS3;

	public List<String> uploadFiles(List<MultipartFile> multipartFiles) {
		return new FileUploader().uploadMultipartFiles(multipartFiles);
	}

	public String uploadFile(MultipartFile multipartFile) {
		return new FileUploader().uploadMultipartFile(multipartFile);
	}

	public String getFileUrl(String fileName) {
		return new FileUrlGenerator().generate(fileName);
	}

	public String uploadAndGetUrl(MultipartFile file) {
		if (file == null)
			return null;
		String fileName = uploadFile(file);
		return getFileUrl(fileName);
	}

	public List<String> uploadAndGetUrls(List<MultipartFile> files) {
		return files.stream()
			.map(this::uploadAndGetUrl)
			.toList();
	}

	private class FileUploader {
		List<String> uploadMultipartFiles(List<MultipartFile> multipartFiles) {
			List<String> fileNameList = new ArrayList<>();
			multipartFiles.forEach(file -> fileNameList.add(uploadMultipartFile(file)));
			return fileNameList;
		}

		String uploadMultipartFile(MultipartFile file) {
			String fileName = new FileNameGenerator().generate(file.getOriginalFilename(), file.getContentType());
			ObjectMetadata metadata = new MetadataGenerator().generate(file);

			try (InputStream inputStream = file.getInputStream()) {
				PutObjectRequest request = RequestBuilder.builder()
					.bucket(bucket)
					.fileName(fileName)
					.inputStream(inputStream)
					.metadata(metadata)
					.build()
					.createRequest();

				amazonS3.putObject(request);
			} catch (IOException e) {
				throw new WeGoException(S3_FILE_UPLOAD_FAILED);
			}

			return fileName;
		}
	}

	private class FileNameGenerator {
		String generate(String originalFileName, String contentType) {
			String randomUUID = UUID.randomUUID().toString();
			String extension = getFileExtension(originalFileName, contentType);
			String date = DateTimeUtils.getYearMonthDay(Instant.now());

			return profile + FOLDER_DELIMITER + date + FOLDER_DELIMITER + randomUUID + extension;
		}

		private String getFileExtension(String fileName, String contentType) {
			try {
				if (fileName.equals("blob") || fileName.lastIndexOf(".") == -1) {
					switch (contentType) {
						case "image/webp":
							return ".webp";
						case "image/jpeg":
							return ".jpg";
						case "image/png":
							return ".png";
						default:
							throw new WeGoException(INVALID_FILE_FORMAT);
					}
				}
				return fileName.substring(fileName.lastIndexOf("."));
			} catch (StringIndexOutOfBoundsException e) {
				throw new WeGoException(INVALID_FILE_FORMAT);
			}
		}
	}

	private static class MetadataGenerator {
		ObjectMetadata generate(MultipartFile file) {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(file.getSize());
			metadata.setContentType(file.getContentType());
			return metadata;
		}
	}

	@Builder
	private static class RequestBuilder {
		private String bucket;
		private String fileName;
		private InputStream inputStream;
		private ObjectMetadata metadata;

		public PutObjectRequest createRequest() {
			return new PutObjectRequest(bucket, fileName, inputStream, metadata);
		}
	}

	private class FileUrlGenerator {
		String generate(String fileName) {
			return amazonS3.getUrl(bucket, fileName).toString();
		}
	}
}
