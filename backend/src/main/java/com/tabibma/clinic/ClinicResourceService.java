package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateClinicResourceRequest;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Story 8.2: clinic admin management of shared resources (rooms/equipment). */
@Service
public class ClinicResourceService {

    private final ClinicRepository clinicRepository;
    private final ClinicResourceRepository clinicResourceRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ClinicStaffMembershipRepository clinicStaffMembershipRepository;

    public ClinicResourceService(ClinicRepository clinicRepository,
                                  ClinicResourceRepository clinicResourceRepository,
                                  DoctorProfileRepository doctorProfileRepository,
                                  ClinicStaffMembershipRepository clinicStaffMembershipRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicResourceRepository = clinicResourceRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.clinicStaffMembershipRepository = clinicStaffMembershipRepository;
    }

    @Transactional
    public ClinicResource createResource(UserContext principal, UUID clinicId, CreateClinicResourceRequest request) {
        getOwnedClinicOrThrow(principal, clinicId);
        ClinicResource resource = new ClinicResource(clinicId, request.type(), request.name());
        return clinicResourceRepository.save(resource);
    }

    /** Story 8.2 Batch 6: a clinic's own doctor-staff can also list resources (active only), so
     * they can pick required rooms/equipment when creating an IN_PERSON availability rule —
     * mirrors {@code DoctorOnboardingService.listMyClinics}' "doctor needs read access to their
     * own clinic's data" precedent. The clinic admin still sees every resource, active or not. */
    public List<ClinicResource> listResources(UserContext principal, UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic not found."));
        if (clinic.getAdminUserId().equals(principal.userId())) {
            return clinicResourceRepository.findAllByClinicId(clinicId);
        }
        boolean isStaffDoctor = doctorProfileRepository.findByUserId(principal.userId())
                .map(profile -> clinicStaffMembershipRepository.existsByClinicIdAndDoctorProfileId(clinicId, profile.getId()))
                .orElse(false);
        if (!isStaffDoctor) {
            throw new ForbiddenException("You don't have access to this clinic's resources.");
        }
        return clinicResourceRepository.findAllByClinicIdAndActiveTrue(clinicId);
    }

    @Transactional
    public ClinicResource deactivateResource(UserContext principal, UUID clinicId, UUID resourceId) {
        getOwnedClinicOrThrow(principal, clinicId);
        ClinicResource resource = clinicResourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found."));
        if (!resource.getClinicId().equals(clinicId)) {
            throw new NotFoundException("Resource not found.");
        }
        resource.deactivate();
        return clinicResourceRepository.save(resource);
    }

    private Clinic getOwnedClinicOrThrow(UserContext principal, UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic not found."));
        if (!clinic.getAdminUserId().equals(principal.userId())) {
            throw new ForbiddenException("You can only manage your own clinic.");
        }
        return clinic;
    }
}
