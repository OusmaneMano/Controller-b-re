package com.controller.service;

import com.controller.entity.Company;
import com.controller.entity.Record;
import com.controller.entity.TableColumn;
import com.controller.entity.User;
import com.controller.entity.enums.CompanyStatus;
import com.controller.entity.enums.FieldRole;
import com.controller.entity.enums.FieldType;
import com.controller.entity.enums.UserRole;
import com.controller.repository.CompanyRepository;
import com.controller.repository.RecordRepository;
import com.controller.repository.TableColumnRepository;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoCompanyService {

    public static final String DEMO_OWNER_EMAIL = "demo@controller.local";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final TableColumnRepository tableColumnRepository;
    private final RecordRepository recordRepository;
    private final MembershipService membershipService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Company getOrCreateDemoCompany() {
        return companyRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getDemo()))
                .findFirst()
                .orElseGet(this::createDemo);
    }

    @Transactional
    public void ensureDemoMembership(User user) {
        if (user.getRole() != UserRole.MANAGER) return;
        Company demo = getOrCreateDemoCompany();
        membershipService.add(user, demo, UserRole.EMPLOYEE);
        if (user.getLastCompanyId() == null) {
            user.setLastCompanyId(demo.getId());
            user.setCompany(demo);
        }
    }

    private Company createDemo() {
        User owner = userRepository.findByEmail(DEMO_OWNER_EMAIL).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(DEMO_OWNER_EMAIL)
                        .firstName("Demo")
                        .lastName("Owner")
                        .password(passwordEncoder.encode("demo-not-for-login"))
                        .role(UserRole.MANAGER)
                        .build()));

        Company company = companyRepository.save(Company.builder()
                .name("Demo Retail Shop")
                .industry("Retail")
                .description("Try CONTROLLER here. Rows reset the idea: Quantity × Unit Price = Amount.")
                .manager(owner)
                .employeeUsername("demo_shop")
                .employeePassword("1234")
                .tableDesign("PROFESSIONAL")
                .status(CompanyStatus.ACTIVE)
                .demo(true)
                .build());

        TableColumn qty = tableColumnRepository.save(col(company, "Quantity", FieldType.NUMBER, FieldRole.QUANTITY, 0, true));
        TableColumn price = tableColumnRepository.save(col(company, "Unit Price", FieldType.NUMBER, FieldRole.UNIT_PRICE, 1, true));
        tableColumnRepository.save(col(company, "Amount", FieldType.NUMBER, FieldRole.AMOUNT, 2, false));
        tableColumnRepository.save(col(company, "Product", FieldType.TEXT, FieldRole.NONE, 3, true));

        seed(company, owner, 3, 500, "Rice 25kg");
        seed(company, owner, 2, 1500, "Cooking oil");
        seed(company, owner, 10, 200, "Soap");
        log.info("Seeded demo company {}", company.getId());
        return company;
    }

    private TableColumn col(Company c, String name, FieldType type, FieldRole role, int order, boolean required) {
        return TableColumn.builder()
                .company(c)
                .fieldName(name)
                .fieldType(type)
                .fieldRole(role)
                .isRequired(required)
                .orderIndex(order)
                .build();
    }

    private void seed(Company company, User owner, double q, double p, String product) {
        Map<String, Object> data = new HashMap<>();
        data.put("Quantity", q);
        data.put("Unit Price", p);
        data.put("Amount", q * p);
        data.put("Product", product);
        recordRepository.save(Record.builder()
                .company(company)
                .createdBy(owner)
                .data(data)
                .deleted(false)
                .status("ACTIVE")
                .build());
    }
}