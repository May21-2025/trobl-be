package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl._global.utility.UrlMaker;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Advertisement advertisement;

    @Enumerated(EnumType.STRING)
    private AdType adType;

    private Integer weight;

    public String getImageUrl() {
        return UrlMaker.makeAdImageUrl(advertisement.getBrandName(), adType);
    }

    public Banner(Advertisement advertisement, AdType adType, Integer weight) {
        this.advertisement = advertisement;
        this.adType = adType;
        this.weight = weight != null ? weight : 1;
    }


}
