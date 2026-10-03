package com.vcc.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringBootTemplateApplication {

    public static void main(String[] args) {
        // Timezone is set via JAVA_TOOL_OPTIONS or OS default
        SpringApplication.run(SpringBootTemplateApplication.class, args);
    }

}
