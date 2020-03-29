package fr.edu.lyon.pdfmerge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import fr.edu.lyon.pdfmerge.storage.commons.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class PdfmergeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfmergeApplication.class, args);
	}

}
