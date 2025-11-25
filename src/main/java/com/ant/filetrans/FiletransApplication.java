package com.ant.filetrans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "File Transfer")
@SpringBootApplication
public class FiletransApplication {

    public static void main(String[] args) {
		SpringApplication.run(FiletransApplication.class, args);
	}

}
