package com.vibechat.domain.auth;

import com.vibechat.domain.UserProvider;

import java.io.Serializable;

public record UserPrincipal (
  Long id,
  String nickname,
  UserProvider provider
) implements Serializable {}
