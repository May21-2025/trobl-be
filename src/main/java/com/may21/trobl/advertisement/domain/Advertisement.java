package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.enums.BannerType;
import com.may21.trobl._global.utility.UrlMaker;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    private BannerType bannerType;

    private Integer weight;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdRecord> adRecords;

    public Advertisement(Brand brand, AdvertisementDto.BannerRequest request) {
        this.brand = brand;
        this.bannerType = BannerType.valueOf(request.type());
        this.weight = request.weight() != null ? request.weight() : 1;
    }

    public String getImageUrl() {
        // 저장된 imageUrl이 있으면 그것을 사용, 없으면 동적으로 생성
        return this.imageUrl != null ? this.imageUrl :
                UrlMaker.makeAdImageUrl(id, brand.getBrandName(),
                bannerType);
    }

    public Advertisement(Brand brand, BannerType bannerType, Integer weight) {
        this.brand = brand;
        this.bannerType = bannerType;
        this.weight = weight != null ? weight : 1;
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public BannerType getBannerType() {
        return bannerType;
    }

    public Integer getWeight() {
        return weight;
    }

    public void updateFromRequest(AdvertisementDto.BannerRequest request) {
        if(request.type() != null) {
            this.bannerType = BannerType.valueOf(request.type());
        }
        if(request.weight() != null) {
            this.weight = request.weight();
        }
    }
}
