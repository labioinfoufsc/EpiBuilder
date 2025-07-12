package ufsc.br.epibuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@EnableAsync
@EnableScheduling
@ComponentScan
public class EpibuilderBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EpibuilderBackendApplication.class, args);
	}

}
