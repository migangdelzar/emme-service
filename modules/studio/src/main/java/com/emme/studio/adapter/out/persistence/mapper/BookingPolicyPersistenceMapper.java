package com.emme.studio.adapter.out.persistence.mapper;

import com.emme.studio.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.studio.domain.model.BookingPolicy;

/** Translates booking-policy domain state to and from JPA. */
public final class BookingPolicyPersistenceMapper {

  public BookingPolicy toDomain(BookingPolicyEntity entity) {
    return BookingPolicy.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getMinNoticeMinutes(),
        entity.getMaxAdvanceDays(),
        entity.getCancellationWindowMinutes(),
        entity.isAllowOverlap());
  }

  public void updateEntity(BookingPolicy domain, BookingPolicyEntity entity) {
    entity.setMinNoticeMinutes(domain.getMinNoticeMinutes());
    entity.setMaxAdvanceDays(domain.getMaxAdvanceDays());
    entity.setCancellationWindowMinutes(domain.getCancellationWindowMinutes());
    entity.setAllowOverlap(domain.isAllowOverlap());
  }

  public BookingPolicyEntity toNewEntity(BookingPolicy domain) {
    BookingPolicyEntity entity =
        new BookingPolicyEntity(
            domain.getTenantId(),
            domain.getMinNoticeMinutes(),
            domain.getMaxAdvanceDays(),
            domain.getCancellationWindowMinutes(),
            domain.isAllowOverlap());
    updateEntity(domain, entity);
    return entity;
  }
}
