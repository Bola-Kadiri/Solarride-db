package com.solarride.solarride.service.onboarding;

import com.solarride.solarride.domain.supplier.ProductCategory;
import com.solarride.solarride.domain.supplier.Supplier;
import com.solarride.solarride.domain.supplier.SupplierDocument;
import com.solarride.solarride.domain.user.Role;
import com.solarride.solarride.domain.user.User;
import com.solarride.solarride.domain.user.UserStatus;
import com.solarride.solarride.dto.request.SupplierRegisterRequest;
import com.solarride.solarride.dto.response.SupplierResponse;
import com.solarride.solarride.exception.DuplicateResourceException;
import com.solarride.solarride.exception.ResourceNotFoundException;
import com.solarride.solarride.repository.SupplierRepository;
import com.solarride.solarride.repository.UserRepository;
import com.solarride.solarride.service.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierOnboardingService {

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3StorageService s3StorageService;

    @Transactional
    public SupplierResponse register(SupplierRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setFirstName(request.repFirstName());
        user.setLastName(request.repLastName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.SUPPLIER);
        user.setStatus(UserStatus.PENDING);
        user = userRepository.save(user);

        Supplier supplier = new Supplier();
        supplier.setUser(user);
        supplier.setCompanyName(request.companyName());
        supplier.setRegistrationNumber(request.registrationNumber());
        supplier.setTaxId(request.taxId());
        supplier.setRepName(request.repFirstName() + " " + request.repLastName());
        supplier.setDeliveryLeadTimeDays(request.deliveryLeadTimeDays());
        supplier.setSlaAccepted(request.slaAccepted());
        if (request.slaAccepted()) {
            supplier.setSlaAcceptedAt(Instant.now());
        }

        if (request.productCategories() != null) {
            for (String cat : request.productCategories()) {
                ProductCategory pc = new ProductCategory();
                pc.setSupplier(supplier);
                pc.setCategory(cat);
                supplier.getProductCategories().add(pc);
            }
        }

        supplier = supplierRepository.save(supplier);
        log.info("Supplier registered: {}, user: {}", supplier.getId(), user.getId());
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse uploadDocument(UUID supplierId, MultipartFile file, String documentType) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        String s3Key = s3StorageService.upload(file, "supplier-docs/" + supplierId);

        SupplierDocument doc = new SupplierDocument();
        doc.setSupplier(supplier);
        doc.setDocumentType(documentType);
        doc.setS3Key(s3Key);
        doc.setOriginalFilename(file.getOriginalFilename());
        supplier.getDocuments().add(doc);

        supplierRepository.save(supplier);
        log.info("Document {} uploaded for supplier {}", documentType, supplierId);
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse approve(UUID supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        User user = supplier.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        supplierRepository.save(supplier);
        log.info("Supplier approved: {}", supplierId);
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse reject(UUID supplierId, String reason) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        User user = supplier.getUser();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("Supplier rejected: {} reason: {}", supplierId, reason);
        return toResponse(supplier);
    }

    public SupplierResponse getProfile(UUID userId) {
        Supplier supplier = supplierRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile not found"));
        return toResponse(supplier);
    }

    private SupplierResponse toResponse(Supplier supplier) {
        User user = supplier.getUser();
        return new SupplierResponse(
                supplier.getId(),
                user.getId(),
                user.getEmail(),
                supplier.getCompanyName(),
                supplier.getRepName(),
                user.getStatus().name(),
                supplier.getAverageRating());
    }
}