package com.commerceinsight.dataimport.service;

import com.commerceinsight.customer.domain.CustomerGender;
import com.commerceinsight.customer.domain.CustomerGroup;
import com.commerceinsight.customer.dto.request.CreateCustomerRequest;
import com.commerceinsight.customer.repository.CustomerGroupRepository;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CustomerImportService — validates and imports a single customer row.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Calls {@link CustomerService#create(CreateCustomerRequest)} — never bypasses it.</li>
 *   <li>Each row runs in {@code PROPAGATION_REQUIRES_NEW} for per-row isolation.</li>
 *   <li>Group is resolved by name (case-insensitive). Null if not provided or not found.</li>
 *   <li>Gender must match enum values: MALE, FEMALE, OTHER (case-insensitive).</li>
 * </ul>
 *
 * <p>Expected CSV headers (case-insensitive):
 * {@code firstname, lastname, email, phone, dateofbirth, gender, groupname}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportService {

    public static final String[] REQUIRED_HEADERS = {"firstname", "lastname"};
    public static final String[] ALL_HEADERS =
            {"firstname", "lastname", "email", "phone", "dateofbirth", "gender", "groupname"};

    private final CustomerService customerService;
    private final CustomerGroupRepository groupRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowImportResult importRow(ParsedRow row) {
        List<RowError> errors = validateRow(row);
        if (!errors.isEmpty()) {
            return RowImportResult.failure(row.getRowNumber(), errors);
        }

        try {
            // Resolve group by name (optional)
            UUID groupId = null;
            String groupName = row.get("groupname");
            if (!groupName.isEmpty()) {
                CustomerGroup group = groupRepository.findByNameIgnoreCase(groupName).orElse(null);
                if (group != null) {
                    groupId = group.getId();
                } else {
                    return RowImportResult.failure(row.getRowNumber(), List.of(
                            new RowError("groupname", groupName,
                                    ImportValidationCode.ENTITY_NOT_FOUND,
                                    "Row %d: Customer group '%s' not found".formatted(row.getRowNumber(), groupName))));
                }
            }

            // Parse optional dateOfBirth
            LocalDate dob = null;
            if (row.hasValue("dateofbirth")) {
                dob = LocalDate.parse(row.get("dateofbirth"));
            }

            // Parse optional gender
            CustomerGender gender = null;
            if (row.hasValue("gender")) {
                gender = CustomerGender.valueOf(row.get("gender").toUpperCase());
            }

            CreateCustomerRequest request = new CreateCustomerRequest(
                    null,   // customerCode — auto-generated
                    row.get("firstname"),
                    row.get("lastname"),
                    row.hasValue("email") ? row.get("email") : null,
                    row.hasValue("phone") ? row.get("phone") : null,
                    dob,
                    gender,
                    groupId
            );

            customerService.create(request);
            log.debug("Customer imported from row {}: email={}", row.getRowNumber(), row.get("email"));
            return RowImportResult.success(row.getRowNumber());

        } catch (DuplicateResourceException e) {
            String field = e.getMessage().contains("email") ? "email" : "customerCode";
            return RowImportResult.failure(row.getRowNumber(), List.of(
                    new RowError(field, row.get(field),
                            ImportValidationCode.DUPLICATE_RECORD,
                            "Row %d: %s".formatted(row.getRowNumber(), e.getMessage()))));
        } catch (Exception e) {
            log.warn("Unexpected error importing customer row {}: {}", row.getRowNumber(), e.getMessage());
            return RowImportResult.failure(row.getRowNumber(), List.of(
                    new RowError(null, null,
                            ImportValidationCode.BUSINESS_RULE_VIOLATION,
                            "Row %d: %s".formatted(row.getRowNumber(), e.getMessage()))));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<RowError> validateRow(ParsedRow row) {
        List<RowError> errors = new ArrayList<>();

        // Required: firstName
        if (!row.hasValue("firstname")) {
            errors.add(new RowError("firstname", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                    "Row %d: 'firstName' is required".formatted(row.getRowNumber())));
        } else if (row.get("firstname").length() > 100) {
            errors.add(new RowError("firstname", row.get("firstname"), ImportValidationCode.VALUE_TOO_LONG,
                    "Row %d: 'firstName' must not exceed 100 characters".formatted(row.getRowNumber())));
        }

        // Required: lastName
        if (!row.hasValue("lastname")) {
            errors.add(new RowError("lastname", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                    "Row %d: 'lastName' is required".formatted(row.getRowNumber())));
        } else if (row.get("lastname").length() > 100) {
            errors.add(new RowError("lastname", row.get("lastname"), ImportValidationCode.VALUE_TOO_LONG,
                    "Row %d: 'lastName' must not exceed 100 characters".formatted(row.getRowNumber())));
        }

        // Optional: email format
        if (row.hasValue("email")) {
            String email = row.get("email");
            if (!email.contains("@") || email.length() > 255) {
                errors.add(new RowError("email", email, ImportValidationCode.INVALID_FORMAT,
                        "Row %d: 'email' is not a valid email address".formatted(row.getRowNumber())));
            }
        }

        // Optional: gender
        if (row.hasValue("gender")) {
            try {
                CustomerGender.valueOf(row.get("gender").toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new RowError("gender", row.get("gender"), ImportValidationCode.INVALID_VALUE,
                        "Row %d: 'gender' must be one of: MALE, FEMALE, OTHER".formatted(row.getRowNumber())));
            }
        }

        // Optional: dateOfBirth
        if (row.hasValue("dateofbirth")) {
            try {
                LocalDate.parse(row.get("dateofbirth"));
            } catch (DateTimeParseException e) {
                errors.add(new RowError("dateofbirth", row.get("dateofbirth"), ImportValidationCode.INVALID_FORMAT,
                        "Row %d: 'dateOfBirth' must be in ISO format yyyy-MM-dd".formatted(row.getRowNumber())));
            }
        }

        return errors;
    }
}
