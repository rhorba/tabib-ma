package com.tabibma.booking;

import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves patient display names for a doctor's own appointment list, so the
 * practice sees who is coming without a second request per row. Only called
 * for the DOCTOR branch of listMine; patients never receive other names.
 */
@Service
public class PatientNameLookup {

    private final UserRepository userRepository;

    public PatientNameLookup(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> namesFor(Collection<Appointment> appointments) {
        var ids = appointments.stream().map(Appointment::getPatientId).collect(Collectors.toSet());
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));
    }
}
