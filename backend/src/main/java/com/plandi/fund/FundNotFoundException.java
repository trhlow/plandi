package com.plandi.fund;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** 404 cho cả quỹ không tồn tại lẫn quỹ của người khác, để không lộ quỹ nào có thật. */
public class FundNotFoundException extends ErrorResponseException {

    public FundNotFoundException() {
        super(HttpStatus.NOT_FOUND);
    }
}
