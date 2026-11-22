package com.plandi.privacy.internal;

import com.plandi.account.AccountService;
import com.plandi.privacy.PersonalDataHandler;
import com.plandi.privacy.Purpose;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Handler cho dữ liệu của privacy và của account. Account không cài handler trực tiếp vì privacy đã phụ thuộc account
 * (sẽ thành vòng); thay vào đó account đưa ra {@code exportData}/{@code deleteData} public và privacy bọc lại ở đây.
 */
@Configuration
class PersonalDataHandlers {

    record ConsentRecord(Purpose purpose, boolean granted, String policyVersion, Instant at) {}

    @Bean
    PersonalDataHandler consentData(ConsentEventRepository consents) {
        return new PersonalDataHandler() {
            @Override
            public String section() {
                return "consents";
            }

            @Override
            public Object export(long accountId) {
                List<ConsentRecord> history = consents.findByAccountIdOrderByIdAsc(accountId).stream()
                        .map(e -> new ConsentRecord(e.purpose(), e.granted(), e.policyVersion(), e.createdAt()))
                        .toList();
                return history;
            }

            @Override
            public void delete(long accountId) {
                consents.deleteByAccountId(accountId);
            }

            @Override
            public int deleteOrder() {
                return 100;
            }
        };
    }

    @Bean
    PersonalDataHandler accountData(AccountService accounts) {
        return new PersonalDataHandler() {
            @Override
            public String section() {
                return "account";
            }

            @Override
            public Object export(long accountId) {
                return accounts.exportData(accountId);
            }

            @Override
            public void delete(long accountId) {
                accounts.deleteData(accountId);
            }

            @Override
            public int deleteOrder() {
                return Integer.MAX_VALUE;
            }
        };
    }
}
