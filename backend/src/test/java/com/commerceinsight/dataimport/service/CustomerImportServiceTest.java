package com.commerceinsight.dataimport.service;

import com.commerceinsight.customer.domain.CustomerGroup;
import com.commerceinsight.customer.dto.request.CreateCustomerRequest;
import com.commerceinsight.customer.repository.CustomerGroupRepository;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CustomerImportServiceTest — unit tests for {@link CustomerImportService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerImportService Unit Tests")
class CustomerImportServiceTest {

    @Mock private CustomerService customerService;
    @Mock private CustomerGroupRepository groupRepository;

    @InjectMocks private CustomerImportService service;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid minimal row — calls CustomerService.create, returns success")
    void validMinimalRow_returnsSuccess() {
        ParsedRow row = row(1, Map.of("firstname", "John", "lastname", "Doe"));
        given(customerService.create(any())).willReturn(null);

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isTrue();
        then(customerService).should().create(any(CreateCustomerRequest.class));
    }

    @Test
    @DisplayName("Valid row with all optional fields — resolved correctly")
    void validFullRow_returnsSuccess() {
        CustomerGroup group = new CustomerGroup();
        group.setId(UUID.randomUUID());
        given(groupRepository.findByNameIgnoreCase("VIP")).willReturn(Optional.of(group));
        given(customerService.create(any())).willReturn(null);

        ParsedRow row = row(1, Map.of(
                "firstname", "John",
                "lastname", "Doe",
                "email", "john@example.com",
                "phone", "+84901234567",
                "dateofbirth", "1990-01-15",
                "gender", "MALE",
                "groupname", "VIP"
        ));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("Group not found — returns failure with ENTITY_NOT_FOUND")
    void groupNotFound_returnsFailure() {
        given(groupRepository.findByNameIgnoreCase("Unknown")).willReturn(Optional.empty());

        ParsedRow row = row(1, Map.of("firstname", "John", "lastname", "Doe", "groupname", "Unknown"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors().get(0).errorCode()).isEqualTo(ImportValidationCode.ENTITY_NOT_FOUND);
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Test
    @DisplayName("Missing firstName — returns failure with MISSING_REQUIRED_FIELD")
    void missingFirstName_returnsFailure() {
        ParsedRow row = row(2, Map.of("lastname", "Doe"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "firstname".equals(e.fieldName())
                && ImportValidationCode.MISSING_REQUIRED_FIELD.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Invalid email format — returns failure with INVALID_FORMAT")
    void invalidEmail_returnsFailure() {
        ParsedRow row = row(3, Map.of("firstname", "John", "lastname", "Doe", "email", "not-an-email"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "email".equals(e.fieldName())
                && ImportValidationCode.INVALID_FORMAT.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Invalid gender — returns failure with INVALID_VALUE")
    void invalidGender_returnsFailure() {
        ParsedRow row = row(4, Map.of("firstname", "John", "lastname", "Doe", "gender", "UNKNOWN_GENDER"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "gender".equals(e.fieldName())
                && ImportValidationCode.INVALID_VALUE.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Invalid dateOfBirth format — returns failure with INVALID_FORMAT")
    void invalidDob_returnsFailure() {
        ParsedRow row = row(5, Map.of("firstname", "John", "lastname", "Doe", "dateofbirth", "31-12-1990"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "dateofbirth".equals(e.fieldName())
                && ImportValidationCode.INVALID_FORMAT.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Duplicate email — returns failure with DUPLICATE_RECORD")
    void duplicateEmail_returnsFailure() {
        given(customerService.create(any()))
                .willThrow(new DuplicateResourceException(
                        ErrorCode.CUSTOMER_EMAIL_ALREADY_EXISTS, "email already exists"));

        ParsedRow row = row(6, Map.of("firstname", "John", "lastname", "Doe", "email", "john@example.com"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> ImportValidationCode.DUPLICATE_RECORD.equals(e.errorCode()));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ParsedRow row(int rowNumber, Map<String, String> values) {
        return new ParsedRow(rowNumber, values);
    }
}
