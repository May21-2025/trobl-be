package com.may21.trobl.admin.domain;

import com.may21.trobl._global.enums.DateType;
import com.may21.trobl._global.enums.LayoutType;
import com.may21.trobl._global.enums.PostSortType;
import com.may21.trobl._global.enums.ScheduleType;
import com.may21.trobl.admin.AdminDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class MainLayoutGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;

    private PostSortType sortType;
    private LayoutType layoutType;
    private DateType dateType;
    private Integer dateInt;

    private String address;
    @Setter
    private int index;

    @Setter
    private ScheduleType scheduleType = ScheduleType.NONE;

    @Setter
    private boolean active = true;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "main_layout_group_tag_ids", joinColumns = @JoinColumn(name = "code"))
    private List<Long> tagIds;



    public MainLayoutGroup(AdminDto.MainLayoutRequest request, int maxIndex) {
        this.name = request.getName();
        this.description = request.getDescription();
        this.sortType = request.getSortType();
        this.layoutType = request.getLayoutType();
        this.dateInt = request.getDateInt();
        this.dateType = request.getDateType();
        this.address = request.getAddress();
        this.index = maxIndex;
        this.tagIds = request.getTagIds();
        this.scheduleType = request.getScheduleType();
    }
}
