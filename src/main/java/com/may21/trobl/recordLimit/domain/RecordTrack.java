package com.may21.trobl.recordLimit.domain;

import com.may21.trobl._global.utility.Timestamped;
import com.may21.trobl.recordLimit.dto.RecordDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RecordTrack extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Unique
    private String recordId;

    private Long userId;

    @Setter
    private boolean aiGenerated;

    public RecordTrack(Long userId, String recordId) {
        this.userId = userId;
        this.recordId = recordId;
        this.aiGenerated = false;
    }

}
