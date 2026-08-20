package com.foodloop.pickup.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.domain.GeoUtils;
import com.foodloop.pickup.domain.VehicleType;
import com.foodloop.pickup.domain.VolunteerProfile;
import com.foodloop.pickup.domain.VolunteerProfileRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VolunteerService {

    private final VolunteerProfileRepository volunteerProfileRepository;

    public VolunteerService(VolunteerProfileRepository volunteerProfileRepository) {
        this.volunteerProfileRepository = volunteerProfileRepository;
    }

    /**
     * The real gate on volunteer actions (see PickupTaskController's
     * Javadoc): registering here is a separate, explicit step from granting
     * the VOLUNTEER realm role in Identity — a role alone doesn't mean
     * someone has actually set up a profile Pickup can assign work to.
     */
    @Transactional
    public VolunteerProfile register(UUID tenantId, UUID userId, VehicleType vehicleType, Integer capacityServings) {
        if (volunteerProfileRepository.findByUserId(userId).isPresent()) {
            throw new ApiException("VOLUNTEER_PROFILE_ALREADY_EXISTS", HttpStatus.CONFLICT,
                    "A volunteer profile already exists for this account.");
        }
        return volunteerProfileRepository.save(new VolunteerProfile(tenantId, userId, vehicleType, capacityServings));
    }

    @Transactional(readOnly = true)
    public VolunteerProfile getByUserId(UUID userId) {
        return volunteerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("VOLUNTEER_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No volunteer profile for this account — register one first."));
    }

    @Transactional
    public VolunteerProfile updateAvailability(UUID userId, boolean available) {
        VolunteerProfile profile = getByUserId(userId);
        profile.updateAvailability(available);
        return volunteerProfileRepository.save(profile);
    }

    @Transactional
    public VolunteerProfile updateLocation(UUID userId, double lat, double lng) {
        VolunteerProfile profile = getByUserId(userId);
        profile.updateLocation(GeoUtils.point(lat, lng));
        return volunteerProfileRepository.save(profile);
    }
}
