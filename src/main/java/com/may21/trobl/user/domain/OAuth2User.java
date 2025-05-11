package com.may21.trobl.user.domain;

import java.security.Principal;
import java.util.Map;

public interface OAuth2User extends Principal {

    Map<String, Object> getAttributes();

    String getName();
}

