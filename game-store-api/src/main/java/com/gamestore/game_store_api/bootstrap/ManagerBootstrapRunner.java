package com.gamestore.game_store_api.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ManagerBootstrapRunner implements ApplicationRunner {

	private final ManagerBootstrapService managerBootstrapService;

	public ManagerBootstrapRunner(ManagerBootstrapService managerBootstrapService) {
		this.managerBootstrapService = managerBootstrapService;
	}

	@Override
	public void run(ApplicationArguments args) {
		managerBootstrapService.bootstrap();
	}
}
