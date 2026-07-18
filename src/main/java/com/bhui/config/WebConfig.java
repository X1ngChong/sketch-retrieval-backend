package com.bhui.config;


import com.bhui.Common.InfoCommon;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author JXS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有请求
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://" + InfoCommon.fuwuqi + ":5173"
                ) // 允许的源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的方法
                .allowedHeaders("*")
                .allowCredentials(true) // 允许发送凭证
                .maxAge(3600); // 预检请求的缓存时间
    }
}