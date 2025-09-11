package com.may21.trobl.recordLimit.service;

import com.may21.trobl.recordLimit.dto.RecordDto;
import org.springframework.transaction.annotation.Transactional;

public interface RecordTrackService {


    @Transactional(readOnly = true)
    RecordDto.Usage getUserUsage(Long userId);

    @Transactional
    boolean trackUserRecord(Long userId, String recordId);

    @Transactional
    RecordDto.Usage trackAiGeneration(Long userId, String recordId);

    @Transactional
    RecordDto.Usage increaseAiLimit(Long userId, String type);
}
