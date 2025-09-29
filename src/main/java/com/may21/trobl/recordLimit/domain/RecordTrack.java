package com.may21.trobl.recordLimit.domain;

import com.may21.trobl._global.utility.Timestamped;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.util.Objects;
import java.util.UUID;

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

    private Boolean fileImported = false;

    public RecordTrack(Long userId, String recordId) {
        this.userId = userId;
        if (Objects.equals(recordId, "FILE")) {
            this.recordId = UUID.randomUUID()
                    .toString();
            this.fileImported = true;
        }
        else this.recordId = recordId;
        this.aiGenerated = false;
    }

    public boolean isFileImported() {
        return fileImported != null && fileImported;
    }
}
