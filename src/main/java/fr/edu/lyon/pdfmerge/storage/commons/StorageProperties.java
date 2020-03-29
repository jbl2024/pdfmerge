package fr.edu.lyon.pdfmerge.storage.commons;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

	private String location;

}
