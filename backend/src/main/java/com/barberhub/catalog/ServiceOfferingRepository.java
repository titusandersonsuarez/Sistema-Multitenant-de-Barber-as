package com.barberhub.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findAllByOrderByNameAsc();

    List<ServiceOffering> findByActiveTrueOrderByNameAsc();
}
