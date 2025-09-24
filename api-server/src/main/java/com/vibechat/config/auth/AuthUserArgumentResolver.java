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
        boolean hasAuthUserAnnotation = parameter.hasParameterAnnotation(AuthUser.class);
        boolean isUserPrincipalType = parameter.getParameterType().equals(UserPrincipal.class);
        boolean supports = hasAuthUserAnnotation && isUserPrincipalType;

        return supports;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        log.info("=== [ArgumentResolver] resolveArgument called ===");
        log.info("Parameter name: {}", parameter.getParameterName());

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request Method: {}", request.getMethod());

        // 쿠키 정보 확인
        if (request.getCookies() != null) {
            log.info("Total cookies count: {}", request.getCookies().length);
            for (var cookie : request.getCookies()) {
                if ("SESSION".equals(cookie.getName()) || "JSESSIONID".equals(cookie.getName())) {
                    log.info("Found session cookie - Name: {}, Value: {}", cookie.getName(), cookie.getValue());
                }
            }
        } else {
            log.warn("No cookies found in request");
        }

        HttpSession session = request.getSession(false);
        log.info("HttpSession exists: {}", session != null);
        if (session != null) {
            log.info("Session ID: {}", session.getId());
            log.info("Session creation time: {}", session.getCreationTime());
            log.info("Session last accessed time: {}", session.getLastAccessedTime());
        }

        AuthUser authUserAnnotation = parameter.getParameterAnnotation(AuthUser.class);
        boolean isRequired = authUserAnnotation.required();
        log.info("AuthUser required: {}", isRequired);

        if (session == null) {
            log.warn("Session is null, isRequired: {}", isRequired);
            if (isRequired) {
                throw new UserNotLoggedInException("인증이 필요합니다.");
            }
            return null;
        }

        UserPrincipal principal = (UserPrincipal) session.getAttribute(USER_PRINCIPAL_ATTRIBUTE);
        log.info("UserPrincipal from session: {}", principal);

        if (principal == null) {
            log.info("UserPrincipal is null, attempting to restore from individual session attributes");

            // 세션에 UserPrincipal이 없는 경우, 기존 세션에서 userId를 찾아보기
            Long userId = (Long) session.getAttribute("userId");
            String nickname = (String) session.getAttribute("nickname");
            String provider = (String) session.getAttribute("provider");

            log.info("Session attributes - userId: {}, nickname: {}, provider: {}", userId, nickname, provider);

            if (userId != null && nickname != null && provider != null) {
                log.info("Restoring UserPrincipal from session attributes");
                UserPrincipal restoredPrincipal = new UserPrincipal(userId , nickname , UserProvider.fromString(provider));

                // 복구된 UserPrincipal을 세션에 다시 저장
                session.setAttribute(USER_PRINCIPAL_ATTRIBUTE, restoredPrincipal);
                principal = restoredPrincipal;
                log.info("Successfully restored UserPrincipal: {}", restoredPrincipal);
            } else {
                log.warn("Cannot restore UserPrincipal - missing session attributes");
                if (isRequired) {
                    throw new UserNotLoggedInException("인증 정보가 유효하지 않습니다.");
                }
            }
        }

        log.info("Final UserPrincipal result: {}", principal);

        // 세션은 있지만 UserPrincipal의 필드가 null인 경우 (잘못된 상태)
        if (principal != null && principal.id() == null && isRequired) {
            log.error("UserPrincipal exists but id is null - corrupted state");
            throw new UserNotLoggedInException("인증 정보가 손상되었습니다.");
        }

        log.info("=== [ArgumentResolver] Returning UserPrincipal: {} ===", principal);
        return principal;
    }
}