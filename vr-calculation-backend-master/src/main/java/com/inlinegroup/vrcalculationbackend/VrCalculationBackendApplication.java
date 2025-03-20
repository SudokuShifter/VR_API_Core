package com.inlinegroup.vrcalculationbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.inlinegroup.vrcalculationbackend.service.WebClientMockConfig;

@SpringBootApplication
@Import(WebClientMockConfig.class)
public class VrCalculationBackendApplication {
    public static void main(String[] args) {
        //System.setProperty("reactor.netty.ioWorkerCount", "1");
        SpringApplication.run(VrCalculationBackendApplication.class, args);
    }
}
