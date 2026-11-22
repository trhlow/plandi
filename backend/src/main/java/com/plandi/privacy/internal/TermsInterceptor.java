package com.plandi.privacy.internal;

import com.plandi.account.CurrentAccount;
import com.plandi.privacy.ConsentService;
import com.plandi.privacy.Purpose;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Khi {@link ConsentService#POLICY_VERSION} tăng, người dùng chưa đồng ý lại TERMS nhận 403 {@code TERMS_REQUIRED} ở mọi
 * API, trừ những API cần để đồng ý lại hoặc rời app: đăng nhập/đăng xuất, xem mình là ai, đồng ý, xuất và xóa dữ liệu.
 */
@Configuration
class TermsInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final ConsentService consents;
    private final CurrentAccount currentAccount;

    TermsInterceptor(ConsentService consents, CurrentAccount currentAccount) {
        this.consents = consents;
        this.currentAccount = currentAccount;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (allowedWithoutTerms(request.getMethod(), request.getRequestURI())) {
            return true;
        }
        // Đến được đây với /api/** ngoài danh sách trên thì Spring Security đã bắt đăng nhập.
        if (!consents.isGranted(currentAccount.id(), Purpose.TERMS)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, "Điều khoản và chính sách đã thay đổi. Hãy đọc và đồng ý lại.");
            problem.setProperty("code", "TERMS_REQUIRED");
            throw new ErrorResponseException(HttpStatus.FORBIDDEN, problem, null);
        }
        return true;
    }

    private static boolean allowedWithoutTerms(String method, String path) {
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/consents")
                || path.equals("/api/v1/areas")
                || path.equals("/api/v1/me/export")
                || (path.equals("/api/v1/me") && (method.equals("GET") || method.equals("DELETE")));
    }
}
