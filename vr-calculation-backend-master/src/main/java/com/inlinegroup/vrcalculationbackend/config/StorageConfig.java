package com.inlinegroup.vrcalculationbackend.config;

import com.inlinegroup.vrcalculationbackend.service.storage.InMemoryValidationDataStorage;
import com.inlinegroup.vrcalculationbackend.service.storage.ValidationDataStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    @Bean
    public ValidationDataStorage validationDataStorage() {
        return new InMemoryValidationDataStorage();
    }
}
