package com.may21.trobl.post.domain;

import com.may21.trobl._global.utility.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
public abstract class ContentEntity extends Timestamped {
  private Long userId;

  @Setter private String title;

  @Setter
  @Column(columnDefinition = "text")
  private String content;

  protected ContentEntity(String title, String content, Long userId) {
    this.title = title;
    this.content = content;
    this.userId = userId;
  }

  protected ContentEntity() {
    super();
  }
}
