package br.unirio.eleicao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EleicaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(EleicaoApplication.class, args);
	}

}
