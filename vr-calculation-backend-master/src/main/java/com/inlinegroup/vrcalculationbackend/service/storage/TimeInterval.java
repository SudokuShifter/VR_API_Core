package com.inlinegroup.vrcalculationbackend.service.storage;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TimeInterval(
        Boolean isRequest,
        LocalDateTime timeStart,
        LocalDateTime timeEnd
) {
}
