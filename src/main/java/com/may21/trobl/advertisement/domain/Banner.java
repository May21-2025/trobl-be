package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl._global.utility.UrlMaker;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Advertisement advertisement;

    @Enumerated(EnumType.STRING)
    private AdType adType;

    private Integer weight;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    public Banner(Advertisement advertisement, AdvertisementDto.BannerRequest request) {
        this.advertisement = advertisement;
        this.adType = AdType.valueOf(request.type());
        this.weight = request.weight() != null ? request.weight() : 1;
    }

    public String getImageUrl() {
        // 저장된 imageUrl이 있으면 그것을 사용, 없으면 동적으로 생성
        return this.imageUrl != null ? this.imageUrl : UrlMaker.makeAdImageUrl(advertisement.getBrandName(), adType);
    }

    public Banner(Advertisement advertisement, AdType adType, Integer weight) {
        this.advertisement = advertisement;
        this.adType = adType;
        this.weight = weight != null ? weight : 1;
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public Advertisement getAdvertisement() {
        return advertisement;
    }

    public AdType getAdType() {
        return adType;
    }

    public Integer getWeight() {
        return weight;
    }

    public void updateFromRequest(AdvertisementDto.BannerRequest request) {
        if(request.type() != null) {
            this.adType = AdType.valueOf(request.type());
        }
        if(request.weight() != null) {
            this.weight = request.weight();
        }
    }
}
