package com.plandi.privacy.internal;

import com.plandi.account.CurrentAccount;
import com.plandi.account.PasswordVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PersonalDataController {

    private final PersonalDataService personalData;
    private final CurrentAccount currentAccount;
    private final PasswordVerifier passwordVerifier;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    PersonalDataController(
            PersonalDataService personalData,
            CurrentAccount currentAccount,
            PasswordVerifier passwordVerifier,
            FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.personalData = personalData;
        this.currentAccount = currentAccount;
        this.passwordVerifier = passwordVerifier;
        this.sessions = sessions;
    }

    record DeleteRequest(String password) {}

    @GetMapping("/api/v1/me/export")
    ResponseEntity<PersonalDataService.DataExport> export() {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("plandi-du-lieu-cua-toi.json")
                                .build()
                                .toString())
                .body(personalData.export(currentAccount.id()));
    }

    @DeleteMapping("/api/v1/me")
    ResponseEntity<Void> delete(@RequestBody DeleteRequest body, HttpServletRequest request) {
        long accountId = currentAccount.id();
        if (!passwordVerifier.matches(accountId, body.password())) {
            throw new ErrorResponseException(
                    HttpStatus.FORBIDDEN,
                    ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Mật khẩu không đúng."),
                    null);
        }
        personalData.deleteEverything(accountId);

        // Hủy phiên hiện tại qua request (để Spring Session không lưu lại nó), rồi xóa các phiên ở thiết bị khác.
        HttpSession current = request.getSession(false);
        String currentId = current == null ? null : current.getId();
        if (current != null) {
            current.invalidate();
        }
        sessions.findByPrincipalName(String.valueOf(accountId)).keySet().stream()
                .filter(id -> !id.equals(currentId))
                .forEach(sessions::deleteById);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
