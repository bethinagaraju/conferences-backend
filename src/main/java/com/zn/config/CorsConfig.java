
package com.zn.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                    "https://renewable-meet-2026.vercel.app",
                    "https://globalrenewablemeet.com",
                    "https://globallopmeet.com",
                    "https://nursingmeet2026.com",
                    "https://api.zynmarketing.xyz",
                    "http://localhost:*",
                    "https://localhost:*",
                    "http://127.0.0.1:*",
                    "http://147.93.102.131:*",
                    "https://polyscienceconference.com/*",
                    "https://polyscienceconference.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Set-Cookie", "Authorization");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://renewable-meet-2026.vercel.app",
            "https://globalrenewablemeet.com",
            "https://globallopmeet.com",
            "https://nursingmeet2026.com",
            "https://api.zynmarketing.xyz",
            "http://localhost:*",
            "https://localhost:*",
            "http://127.0.0.1:*",
            "http://147.93.102.131:*",
            "https://polyscienceconference.com/*",
            "https://polyscienceconference.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
