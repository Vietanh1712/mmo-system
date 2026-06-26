package config;

import java.util.List;
import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;
import org.springframework.context.annotation.Configuration;
<<<<<<< Updated upstream
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
=======
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
>>>>>>> Stashed changes
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
<<<<<<< Updated upstream
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = new java.io.File("uploads").getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
=======
    // Disabled explicit redirect to let Spring Boot auto-serve static/index.html as Welcome Page

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new SpecificationArgumentResolver());
>>>>>>> Stashed changes
    }
}