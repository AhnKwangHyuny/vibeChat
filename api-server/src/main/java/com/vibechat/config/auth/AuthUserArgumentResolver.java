package com.vibechat.config.auth;

import com.vibechat.config.AuthUser;
import com.vibechat.domain.UserProvider;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.exception.auth.UserNotLoggedInException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_PRINCIPAL_ATTRIBUTE = "userPrincipal";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class) &&
               parameter.getParameterType().equals(UserPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        HttpSession session = request.getSession(false);

        AuthUser authUserAnnotation = parameter.getParameterAnnotation(AuthUser.class);
        boolean isRequired = authUserAnnotation.required();

        if (session == null) {
            if (isRequired) {
                throw new UserNotLoggedInException("인증이 필요합니다.");
            }
            return null;
        }

        UserPrincipal principal = (UserPrincipal) session.getAttribute(USER_PRINCIPAL_ATTRIBUTE);

        System.out.println("1principal.toString() = " + principal.toString());

        if (principal == null) {
            // 세션에 UserPrincipal이 없는 경우, 기존 세션에서 userId를 찾아보기
            Long userId = (Long) session.getAttribute("userId");
            String nickname = (String) session.getAttribute("nickname");
            String provider = (String) session.getAttribute("provider");

            if (userId != null && nickname != null && provider != null) {

                UserPrincipal restoredPrincipal = new UserPrincipal(userId , nickname , UserProvider.fromString(provider));

                // 복구된 UserPrincipal을 세션에 다시 저장
                session.setAttribute(USER_PRINCIPAL_ATTRIBUTE, restoredPrincipal);
            } else if (isRequired) {
                throw new UserNotLoggedInException("인증 정보가 유효하지 않습니다.");
            }
        }

        System.out.println("2principal.toString() = " + principal.toString());

        // 세션은 있지만 UserPrincipal의 필드가 null인 경우 (잘못된 상태)
        if (principal != null && principal.id() == null && isRequired) {
            throw new UserNotLoggedInException("인증 정보가 손상되었습니다.");
        }

        return principal;
    }
}
