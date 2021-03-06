package com.konkerlabs.platform.registry.web.controllers;

import com.konkerlabs.platform.registry.config.GoogleAnalyticsConfig;
import com.konkerlabs.platform.registry.config.HotjarConfig;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice("com.konkerlabs.platform.registry.web.controllers")
public class WebPluginsController {

	private HotjarConfig hotjarConfig = new HotjarConfig();

	private GoogleAnalyticsConfig googleAnalyticsConfig = new GoogleAnalyticsConfig();

	@ModelAttribute
	public void setupConfig(Model model) {
		model.addAttribute("hotjarEnable", hotjarConfig.isEnable());
		model.addAttribute("hotjarId", hotjarConfig.getId());

		model.addAttribute("googleAnalyticsEnable", googleAnalyticsConfig.isEnable());
		model.addAttribute("googleAnalyticsId", googleAnalyticsConfig.getId());
	}

}
