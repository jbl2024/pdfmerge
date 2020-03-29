package fr.edu.lyon.pdfmerge.storage.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import fr.edu.lyon.pdfmerge.storage.exceptions.StorageException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Data
@Slf4j
public class TempStorageService {

	Path rootLocation;

	public void init() {
		Path tempDirWithPrefix;
		try {
			tempDirWithPrefix = Files.createTempDirectory(this.randomString());
			setRootLocation(tempDirWithPrefix);
		} catch (IOException e) {
			log.error("Cannot create temp dir", e);
		}
	}

	public void destroy() {
		try {
			FileUtils.deleteDirectory(getRootLocation().toFile());
		} catch (IOException e) {
			log.error("Cannot delete temp dir", e);
		}

	}

	public File store(MultipartFile file) {
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			if (filename.contains("..")) {
				// This is a security check
				throw new StorageException(
						"Cannot store file with relative path outside current directory " + filename);
			}
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, this.getRootLocation().resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			}
			return this.getRootLocation().resolve(filename).toFile();
		} catch (IOException e) {
			throw new StorageException("Failed to store file " + filename, e);
		}
	}

	private String randomString() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
}
