package com.may21.trobl.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class UserDto {

  @Getter
  @AllArgsConstructor
  public static class Info {
    private final Long userId;
    private final String name;
    private final String createdAt;
    private final String updatedAt;
  }

  @Getter
  public class InfoDetail extends Info {
    private final String email;
    private final String phoneNumber;
    private final String address;

    public InfoDetail(
        Long userId,
        String name,
        String createdAt,
        String updatedAt,
        String email,
        String phoneNumber,
        String address) {
      super(userId, name, createdAt, updatedAt);
      this.email = email;
      this.phoneNumber = phoneNumber;
      this.address = address;
    }
  }

  @Getter
  public class AlertSetting {
    private final boolean emailNotification;
    private final boolean pushNotification;

    public AlertSetting(boolean emailNotification, boolean pushNotification) {
      this.emailNotification = emailNotification;
      this.pushNotification = pushNotification;
    }
  }

  @Getter
  public class InfoRequest {
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final String address;

    public InfoRequest(String name, String email, String phoneNumber, String address) {
      this.name = name;
      this.email = email;
      this.phoneNumber = phoneNumber;
      this.address = address;
    }
  }
}
