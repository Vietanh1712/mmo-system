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
        FileTemplateResolver resolver = new FileTemplateResolver();
        File f1 = new File("apps/frontend/templates");
        File f2 = new File("../frontend/templates");
        File f3 = new File("frontend/templates");
        
        String prefixPath;
        if (f1.exists()) {
            prefixPath = f1.getAbsolutePath() + File.separator;
        } else if (f2.exists()) {
            prefixPath = f2.getAbsolutePath() + File.separator;
        } else if (f3.exists()) {
            prefixPath = f3.getAbsolutePath() + File.separator;
        } else {
            prefixPath = "apps/frontend/templates/";
        }

        resolver.setPrefix(prefixPath);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
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
        
        String staticPath;
        if (static1.exists()) {
            staticPath = static1.getAbsolutePath();
        } else if (static2.exists()) {
            staticPath = static2.getAbsolutePath();
        } else if (static3.exists()) {
            staticPath = static3.getAbsolutePath();
        } else {
            staticPath = "apps/frontend/static";
        }

        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + staticPath + File.separator, "classpath:/static/");
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