package fr.edu.lyon.pdfmerge.storage.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.edu.lyon.pdfmerge.pdf.services.PdfService;
import fr.edu.lyon.pdfmerge.storage.services.FileDescription;
import fr.edu.lyon.pdfmerge.storage.services.StorageService;
import fr.edu.lyon.pdfmerge.storage.services.TempStorageService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/files")
public class FileController {

	@Autowired
	PdfService pdfService;

	public FileController() {
	}

	@GetMapping("")
	public String index(Model model) {
		return "index";
	}

	@PostMapping("/upload-merge")
	@ResponseBody
	public ResponseEntity<Resource> uploadAndMerge(@RequestParam("files") MultipartFile[] files)
			throws IOException, TikaException {
		List<File> items = new ArrayList<File>();
		StorageService storage = new TempStorageService();
		storage.init();

		for (MultipartFile file : files) {
			items.add(storage.storeMultipartFile(file));
		}

		String filename = "output.pdf";

		InputStream mergedPdf = pdfService.merge(pdfService.normalizeSources(items));
		InputStreamResource resource = new InputStreamResource(mergedPdf);
		storage.destroy();

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
	}

	@PostMapping(value = "/upload-api")
	@ResponseBody

	public ResponseEntity<Resource> uploadApi(@RequestBody String payload) throws IOException, TikaException {

		List<File> items = new ArrayList<File>();
		StorageService storage = new TempStorageService();
		storage.init();

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(payload);
		Iterator<Entry<String, JsonNode>> fields = rootNode.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> element = fields.next();
			JsonNode fileNode = element.getValue();
			if (!fileNode.has("filename") || !fileNode.has("content")) {
				continue;
			}
			FileDescription fd = new FileDescription();
			fd.setFilename(fileNode.get("filename").asText());
			fd.setContent(fileNode.get("content").asText());
			items.add(storage.storeFileDescription(fd));
		}

		InputStream mergedPdf = pdfService.merge(pdfService.normalizeSources(items));
		InputStreamResource resource = new InputStreamResource(mergedPdf);
		storage.destroy();

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/pdf").body(resource);
	}

}
