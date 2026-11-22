package com.plandi.privacy.internal;

import com.plandi.account.CurrentAccount;
import com.plandi.privacy.ConsentService;
import com.plandi.privacy.Purpose;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consents")
class ConsentController {

    private final ConsentService consents;
    private final CurrentAccount currentAccount;

    ConsentController(ConsentService consents, CurrentAccount currentAccount) {
        this.consents = consents;
        this.currentAccount = currentAccount;
    }

    record ConsentsView(String policyVersion, Map<Purpose, Boolean> consents) {}

    record ConsentChange(Boolean granted) {}

    @GetMapping
    ConsentsView get() {
        return new ConsentsView(ConsentService.POLICY_VERSION, consents.current(currentAccount.id()));
    }

    @PutMapping("/{purpose}")
    ResponseEntity<Void> put(@PathVariable String purpose, @RequestBody ConsentChange body) {
        Purpose parsed;
        try {
            parsed = Purpose.valueOf(purpose);
        } catch (IllegalArgumentException unknown) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST);
        }
        if (body.granted() == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST);
        }
        consents.set(currentAccount.id(), parsed, body.granted());
        return ResponseEntity.noContent().build();
    }
}
