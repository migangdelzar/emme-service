package com.emme.documents.adapter.out.persistence.repository;

import com.emme.documents.adapter.out.persistence.entity.DocumentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {}
