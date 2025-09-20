package com.vibechat.config.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션이 붙은 컨트롤러 파라미터에 UserPrincipal 객체를 주입하도록 지시합니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
    /**
     * 세션에 사용자 정보가 반드시 필요한지 여부.
     * true일 경우, 인증 정보가 없으면 예외가 발생한다.
     * false일 경우, 인증 정보가 없으면 null이 주입된다.
     */
    boolean required() default true;
}
