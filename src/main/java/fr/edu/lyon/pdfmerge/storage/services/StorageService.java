package fr.edu.lyon.pdfmerge.storage.services;

import java.io.File;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
	void init();
	void destroy();
	public File store(MultipartFile file);
}
