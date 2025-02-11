package com.auto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
	
	
	@GetMapping("/field-info")
	public String fieldInfo(Model model) {
		return "field_info";
	}
	
	@GetMapping("/images-viewer")
	public String imagesViewer(Model model) {
		return "images_viewer";
	}

}
