package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.zif.uom.UomDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CanonicalFactorService {
    private final ZifUomService zifUomService;
    private final Map<String, Mono<Double>> canonicalFactorValues;

    public CanonicalFactorService(ZifUomService zifUomService) {
        this.zifUomService = zifUomService;
        canonicalFactorValues = new ConcurrentHashMap<>();
    }

    public Mono<Double> getCFV(String uid) {
        if (canonicalFactorValues.containsKey(uid)) {
            return canonicalFactorValues.get(uid);
        }
        return addCFV(uid);
    }

    @Scheduled(cron = "#{@getCFCronValue}")
    public void reloadCanonicalFactors() {
        log.info("---------------------- SCHEDULER CANONICAL FACTOR ---------------------------");
        for (String uid : canonicalFactorValues.keySet()) {
            addCFV(uid).subscribe();
        }
    }

    private Mono<Double> addCFV(String uid) {
        Mono<Double> value = zifUomService.getUomById(uid, "PropertyID: " + uid)
                .map(UomDto::canonicalFactor);
        canonicalFactorValues.put(uid, value.cache());
        return value;
    }
}
