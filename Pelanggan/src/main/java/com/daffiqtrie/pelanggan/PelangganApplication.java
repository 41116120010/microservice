package com.daffiqtrie.pelanggan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PelangganApplication {

	public static void main(String[] args) {
		SpringApplication.run(PelangganApplication.class, args);
	}

}
