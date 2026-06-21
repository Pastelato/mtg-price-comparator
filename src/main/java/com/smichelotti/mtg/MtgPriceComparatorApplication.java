package com.smichelotti.mtg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
public class MtgPriceComparatorApplication {

	public static void main(String[] args) {

		SpringApplication.run(
				MtgPriceComparatorApplication.class,
				args);
	}
}
