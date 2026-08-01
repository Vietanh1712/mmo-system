package com.mmo.shared.config;

import java.io.File;
import java.util.List;
import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import com.mmo.shared.security.MaintenanceInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MaintenanceInterceptor maintenanceInterceptor;

    public WebConfig(MaintenanceInterceptor maintenanceInterceptor) {
        this.maintenanceInterceptor = maintenanceInterceptor;
    }

    @Bean
    public ITemplateResolver customTemplateResolver() {
        File f1 = new File("apps/frontend/templates");
        File f2 = new File("../frontend/templates");
        File f3 = new File("frontend/templates");

        File targetDir = f1.exists() ? f1 : (f2.exists() ? f2 : (f3.exists() ? f3 : null));
        if (targetDir != null) {
            FileTemplateResolver resolver = new FileTemplateResolver();
            resolver.setPrefix(targetDir.getAbsolutePath() + File.separator);
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(false);
            resolver.setCheckExistence(true);
            return resolver;
        }

        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = new File("uploads").toURI().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        File static1 = new File("apps/frontend/static");
        File static2 = new File("../frontend/static");
        File static3 = new File("frontend/static");

        if (static1.exists()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + static1.getAbsolutePath() + File.separator, "classpath:/static/");
        } else if (static2.exists()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + static2.getAbsolutePath() + File.separator, "classpath:/static/");
        } else if (static3.exists()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + static3.getAbsolutePath() + File.separator, "classpath:/static/");
        } else {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceInterceptor);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new SpecificationArgumentResolver());
    }
}