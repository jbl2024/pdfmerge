package fr.edu.lyon.pdfmerge.storage.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

import fr.edu.lyon.pdfmerge.storage.commons.FileResponse;
import fr.edu.lyon.pdfmerge.storage.services.StorageService;

@Controller
@RequestMapping("/files")
public class FileController {

	private StorageService storageService;

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
	public String merge() {
		return "redirect:/files/";
	}
	
}
