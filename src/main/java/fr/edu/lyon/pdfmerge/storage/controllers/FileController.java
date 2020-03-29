package fr.edu.lyon.pdfmerge.storage.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import fr.edu.lyon.pdfmerge.pdf.services.PdfService;
import fr.edu.lyon.pdfmerge.storage.services.StorageService;
import fr.edu.lyon.pdfmerge.storage.services.TempStorageService;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/files")
@Slf4j
public class FileController {

	private StorageService storageService;
	
	@Autowired
	PdfService pdfService;

	public FileController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("")
	public String listAllFiles(Model model) {

		model.addAttribute("files",
				storageService
						.loadAll().map(path -> ServletUriComponentsBuilder.fromCurrentContextPath()
								.path("/files/download/").path(path.getFileName().toString()).toUriString())
						.collect(Collectors.toList()));

		return "listFiles";
	}

	@GetMapping("/download/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {

		Resource resource = storageService.loadAsResource(filename);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@PostMapping("/upload-file")
	public String uploadFile(@RequestParam("file") MultipartFile file) {
		storageService.store(file);
		return "redirect:/files/";
	}

	@PostMapping("/upload-multiple-files")
	public String uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
		for (MultipartFile file : files) {
			storageService.store(file);
		}
		return "redirect:/files/";
	}

	@PostMapping("/merge")
	@ResponseBody
	public ResponseEntity<Resource> merge() throws IOException, TikaException {
		List<File> items = new ArrayList<File>();
		storageService.loadAll().forEach(path -> {
			items.add(storageService.load(path.toString()).toFile());
		});
		
		String filename = "output.pdf";
		
		InputStream mergedPdf = pdfService.merge(pdfService.normalizeSources(items));
		InputStreamResource resource = new InputStreamResource(mergedPdf);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
		
	}
	
	@PostMapping("/upload-merge")
	@ResponseBody
	public ResponseEntity<Resource> uploadAndMerge(@RequestParam("files") MultipartFile[] files) throws IOException, TikaException {
		List<File> items = new ArrayList<File>();
		TempStorageService storage = new TempStorageService();
		storage.init();
		
		for (MultipartFile file : files) {
			items.add(storage.store(file));
		}
		
		String filename = "output.pdf";
		
		InputStream mergedPdf = pdfService.merge(pdfService.normalizeSources(items));
		InputStreamResource resource = new InputStreamResource(mergedPdf);
		storage.destroy();

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
	}
	

}
