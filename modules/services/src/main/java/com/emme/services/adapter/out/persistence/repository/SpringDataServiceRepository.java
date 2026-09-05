package com.emme.services.adapter.out.persistence.repository;

import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.domain.model.ServiceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataServiceRepository extends JpaRepository<ServiceEntity, UUID> {
  List<ServiceEntity> findByStatus(ServiceStatus status);
}
