package com.may21.trobl.admin.domain;

import com.may21.trobl._global.enums.DateType;
import com.may21.trobl._global.enums.LayoutType;
import com.may21.trobl._global.enums.PostSortType;
import com.may21.trobl.admin.AdminDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class MainLayoutGroup {
    @Id
    private String code;
    private String name;
    private String description;

    private PostSortType sortType;
    private LayoutType layoutType;
    private Integer dateInt;
    private DateType dateType;

    private String address;
    private int index;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "main_layout_group_tag_ids", joinColumns = @JoinColumn(name = "code"))
    private List<Long> tagIds;



    public MainLayoutGroup(AdminDto.MainLayoutRequest request, int maxIndex) {
        this.code = request.getCode();
        this.name = request.getName();
        this.description = request.getDescription();
        this.sortType = request.getSortType();
        this.layoutType = request.getLayoutType();
        this.dateInt = request.getDateInt();
        this.dateType = request.getDateType();
        this.address = request.getAddress();
        this.index = maxIndex;
        this.tagIds = request.getTagIds();
    }
}
