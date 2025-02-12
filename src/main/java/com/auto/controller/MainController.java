package com.auto.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.auto.service.ExcelService;

@Controller
public class MainController {
	
	@Autowired
    private ExcelService excelService;
	
	@GetMapping("/")
	public String main(Model model) throws IOException {
		List<Map<String, String>> selectList = excelService.readSelectData();
		System.out.println("selectList = "+selectList.toString());
		model.addAttribute("selectList", selectList);
		
		return "index";
	}
	
	@GetMapping("/field-info")
	public String fieldInfo(Model model) {
		return "field_info";
	}
	
	@GetMapping("/region-info")
	public String regionInfo(Model model) throws IOException {
		List<Map<String, String>> selectList = excelService.readSelectData();
		System.out.println("selectList = "+selectList.toString());
		model.addAttribute("selectList", selectList);
		return "region_info";
	}
	
	@GetMapping("/images-viewer")
	public String imagesViewer(Model model) {
		return "images_viewer";
	}
	
	

}
