package io.blackbox.backend.repository;

import io.blackbox.backend.model.DependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependencyRepository extends JpaRepository<DependencyEntity, Long> {

    Optional<DependencyEntity> findBySourceServiceAndDestinationServiceAndDependencyType(
            String sourceService, String destinationService, String dependencyType);

    List<DependencyEntity> findBySourceService(String sourceService);

    List<DependencyEntity> findByDestinationService(String destinationService);
}
