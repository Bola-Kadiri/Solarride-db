package com.solarride.solarride.service.onboarding;

import com.solarride.solarride.domain.installer.Certification;
import com.solarride.solarride.domain.installer.CertificationType;
import com.solarride.solarride.domain.installer.Installer;
import com.solarride.solarride.domain.installer.InstallerBadge;
import com.solarride.solarride.domain.user.Role;
import com.solarride.solarride.domain.user.User;
import com.solarride.solarride.domain.user.UserStatus;
import com.solarride.solarride.dto.request.InstallerRegisterRequest;
import com.solarride.solarride.dto.response.InstallerResponse;
import com.solarride.solarride.exception.DuplicateResourceException;
import com.solarride.solarride.exception.ResourceNotFoundException;
import com.solarride.solarride.repository.InstallerRepository;
import com.solarride.solarride.repository.UserRepository;
import com.solarride.solarride.service.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallerOnboardingService {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final UserRepository userRepository;
    private final InstallerRepository installerRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3StorageService s3StorageService;

    @Transactional
    public InstallerResponse register(InstallerRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.INSTALLER);
        user.setStatus(UserStatus.PENDING);
        user = userRepository.save(user);

        Installer installer = new Installer();
        installer.setUser(user);
        installer.setCompanyName(request.companyName());
        installer.setCacNumber(request.cacNumber());
        installer.setTaxId(request.taxId());
        installer.setShopAddress(request.shopAddress());
        installer.setBadge(InstallerBadge.NEW_INSTALLER);
        installer.setSlaAccepted(request.slaAccepted());
        if (request.slaAccepted()) {
            installer.setSlaAcceptedAt(Instant.now());
        }

        if (request.latitude() != null && request.longitude() != null) {
            Point point = GF.createPoint(new Coordinate(request.longitude(), request.latitude()));
            installer.setLocation(point);
        }

        installer = installerRepository.save(installer);
        log.info("Installer registered: {}, user: {}", installer.getId(), user.getId());
        return toResponse(installer);
    }

    @Transactional
    public InstallerResponse uploadDocument(UUID installerId, MultipartFile file, CertificationType documentType) {
        Installer installer = installerRepository.findById(installerId)
                .orElseThrow(() -> new ResourceNotFoundException("Installer", installerId));

        String s3Key = s3StorageService.upload(file, "installer-docs/" + installerId);

        Certification cert = new Certification();
        cert.setInstaller(installer);
        cert.setDocumentType(documentType);
        cert.setS3Key(s3Key);
        cert.setOriginalFilename(file.getOriginalFilename());
        installer.getCertifications().add(cert);

        installerRepository.save(installer);
        log.info("Document {} uploaded for installer {}", documentType, installerId);
        return toResponse(installer);
    }

    @Transactional
    public InstallerResponse approve(UUID installerId) {
        Installer installer = installerRepository.findById(installerId)
                .orElseThrow(() -> new ResourceNotFoundException("Installer", installerId));

        User user = installer.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        installer.setBadge(InstallerBadge.NEW_INSTALLER);
        installerRepository.save(installer);
        log.info("Installer approved: {}", installerId);
        return toResponse(installer);
    }

    @Transactional
    public InstallerResponse reject(UUID installerId, String reason) {
        Installer installer = installerRepository.findById(installerId)
                .orElseThrow(() -> new ResourceNotFoundException("Installer", installerId));

        User user = installer.getUser();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("Installer rejected: {} reason: {}", installerId, reason);
        return toResponse(installer);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('INSTALLER') or hasRole('ADMIN')")
    public InstallerResponse getProfile(UUID userId) {
        Installer installer = installerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Installer profile not found"));
        return toResponse(installer);
    }

    @Transactional(readOnly = true)
    public InstallerResponse getProfileByInstallerId(UUID installerId) {
        Installer installer = installerRepository.findById(installerId)
                .orElseThrow(() -> new ResourceNotFoundException("Installer", installerId));
        return toResponse(installer);
    }

    @Transactional
    public InstallerResponse toggleAvailability(UUID userId) {
        Installer installer = installerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Installer profile not found"));
        installer.setAvailable(!installer.isAvailable());
        installerRepository.save(installer);
        return toResponse(installer);
    }

    private InstallerResponse toResponse(Installer installer) {
        User user = installer.getUser();
        return new InstallerResponse(
                installer.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                installer.getCompanyName(),
                installer.getShopAddress(),
                installer.getBadge().name(),
                user.getStatus().name(),
                installer.getAverageRating(),
                installer.getCompletedJobsCount(),
                installer.isAvailable());
    }
}