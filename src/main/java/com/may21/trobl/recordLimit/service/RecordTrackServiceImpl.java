package com.may21.trobl.recordLimit.service;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.recordLimit.domain.RecordLimit;
import com.may21.trobl.recordLimit.domain.RecordTrack;
import com.may21.trobl.recordLimit.dto.RecordDto;
import com.may21.trobl.recordLimit.repository.RecordLimitRepository;
import com.may21.trobl.recordLimit.repository.RecordTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
        int usage = getUsage(userId);
        int aiLimit = getAiLimit(userId);
        if (usage >= aiLimit) {
            throw new BusinessException(ExceptionCode.AI_REPORT_LIMIT_EXCEEDED);
        }
        RecordTrack recordTrack = Objects.equals(recordId, "FILE") ?
                recordTrackRepository.save(new RecordTrack(userId, recordId)) :
                recordTrackRepository.findByRecordId(recordId)
                        .orElse(null);
        if (recordTrack == null || recordTrack.isAiGenerated()) {
            log.error("already reported");
            new RecordDto.Usage(userId, aiLimit, usage);
        }
        else recordTrack.setAiGenerated(true);
        return new RecordDto.Usage(userId, aiLimit, usage + 1);
    }

    @Override
    public RecordDto.Usage increaseAiLimit(Long userId, String type) {
        return null;
    }
}
