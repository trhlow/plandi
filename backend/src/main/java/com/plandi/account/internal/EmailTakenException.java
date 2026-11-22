package com.plandi.account.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EmailTakenException extends ErrorResponseException {

    public EmailTakenException() {
        super(
                HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email này đã được đăng ký."),
                null);
    }
}
