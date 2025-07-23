package com.may21.trobl._global.enums;

import com.may21.trobl.post.domain.FairView;

public enum FairViewStatus {
    REQUESTED,
    WRITING,
    CONFIRMED,
    ;

    public static FairViewStatus getStatusByEntity(FairView fairView) {
        if(fairView == null) return null;
        if(fairView.isConfirmed()) {
            return CONFIRMED;
        } else if(fairView.getContent()!= null && !fairView.getContent().isEmpty()) {
            return WRITING;
        } else {
            return REQUESTED;
        }
    }
}
