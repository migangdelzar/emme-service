package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AvailableSlot;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Finds available appointment slots for a tenant and date. */
public interface FindAvailableSlotsUseCase {

  List<AvailableSlot> find(UUID tenantId, UUID serviceId, LocalDate date);
}
