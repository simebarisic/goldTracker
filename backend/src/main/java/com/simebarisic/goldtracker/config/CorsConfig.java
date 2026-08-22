package com.simebarisic.goldtracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// POPRAVAK: ovo se aktivira SAMO uz profil "dev" (npr. "mvn spring-boot:run
// -Dspring-boot.run.profiles=dev"), za slucaj kad Vite dev server (5173)
// direktno gadja backend (8080) bez proxija.
//
// U produkciji (Docker, bez aktivnog profila) ova klasa se uopce ne ucitava,
// pa Spring nema registriranu CORS provjeru i ne odbija zahtjeve na temelju
// Origin zaglavlja - sto je tocno ono sto zelimo, jer u produkciji Nginx
// posluzuje frontend i proxira /api na backend (isto porijeklo iz preglednika,
// bez obzira pristupa li se preko localhost ili IP adrese servera). Prije je
// ova konfiguracija bila uvijek aktivna i blokirala je POST/PATCH/DELETE
// zahtjeve iz Nginx-a (Origin http://localhost:8080 nije bio na popisu) s 403.
@Configuration
@Profile("dev")
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS");
    }
}
