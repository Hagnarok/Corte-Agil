package br.edu.unifio.corteagil_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorteAgilApplication {
  public static void main(String[] args) {
    SpringApplication.run(CorteAgilApplication.class, args);
  }
}
