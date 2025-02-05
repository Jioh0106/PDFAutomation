package com.auto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
	
	
	@GetMapping("/field-pos")
	public String fieldPos(Model model) {
		return "field_pos";
	}

}
