package com.emme.salon.application.port.out;

import com.emme.salon.domain.model.DayOfWeek;
import com.emme.salon.domain.model.OperatingHours;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability for tenant operating hours. */
public interface OperatingHoursRepository {

  OperatingHours save(OperatingHours operatingHours);

  Optional<OperatingHours> findByTenantIdAndDayOfWeek(UUID tenantId, DayOfWeek dayOfWeek);

  List<OperatingHours> findByTenantId(UUID tenantId);
}
