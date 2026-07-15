package com.gamestore.game_store_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GameStoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameStoreApiApplication.class, args);
	}

}
