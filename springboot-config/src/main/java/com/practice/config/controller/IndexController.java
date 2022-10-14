package com.practice.config.controller;

import com.practice.config.config.BlogProperties;
import com.practice.config.config.ConfigBean;
import com.practice.config.config.TestConfigBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class IndexController {
	@Autowired
	private BlogProperties blogProperties;
	@Autowired
	private ConfigBean configBean;
	@Autowired
	private TestConfigBean testConfigBean;
	
	@RequestMapping("/")
	String index() {
		return testConfigBean.getName()+"，"+testConfigBean.getAge();
	}
}
