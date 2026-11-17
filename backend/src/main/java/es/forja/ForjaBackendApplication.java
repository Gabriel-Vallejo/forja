package es.forja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Sin el usuario en memoria con contraseña generada de Spring Security: aquí solo se entra con JWT (ADR 0009).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ForjaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForjaBackendApplication.class, args);
	}

}
