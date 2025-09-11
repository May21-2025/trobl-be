package com.may21.trobl.recordLimit.service;

import com.may21.trobl.recordLimit.domain.RecordLimit;
import com.may21.trobl.recordLimit.domain.RecordTrack;
import com.may21.trobl.recordLimit.dto.RecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordTrackServiceImpl implements RecordTrackService {

    private final RecordLimitRepository recordLimitRepository;
    private final RecordTrackRepository recordTrackRepository;

    int AI_LIMIT = 5;

    @Override
    public RecordDto.Usage getUserUsage(Long userId) {

        int aiLimit = getAiLimit(userId);
        // 당월 생성된 AI 트랙 수
        int usage = getUsage(userId);

        return new RecordDto.Usage(userId, aiLimit, usage);
    }

    private int getUsage(Long userId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisMonth = now.withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay();
        return recordTrackRepository.countByUserIdAndAiGeneratedIsTrueAndCreatedAtAfter(userId,
                thisMonth);
    }

    private int getAiLimit(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (recordLimitRepository.existsByUserIdAndExpiryDateBefore(userId, now)) {
            List<RecordLimit> limits = recordLimitRepository.findByUserId(userId);
            for (RecordLimit limit : limits) {
                AI_LIMIT += limit.getAddedCount();
            }
        }
        return AI_LIMIT;
    }

    @Override
    public boolean trackUserRecord(Long userId, String recordId) {
        if (recordTrackRepository.existsByRecordId(recordId)) {
            return false;
        }
        RecordTrack recordTrack = new RecordTrack(userId, recordId);
        recordTrackRepository.save(recordTrack);
        return true;
    }

    @Override
    public RecordDto.Usage trackAiGeneration(Long userId, String recordId) {
        RecordTrack recordTrack = recordTrackRepository.findByRecordId(recordId)
                .orElse(null);
        if (recordTrack == null) {
            log.error("not exist");
            return null;
        }
        recordTrack.setAiGenerated(true);
        int usage = getUsage(userId);
        int aiLimit = getAiLimit(userId);
        return new RecordDto.Usage(userId, aiLimit, usage);
    }

    @Override
    public RecordDto.Usage increaseAiLimit(Long userId, String type) {
        return null;
    }
}
