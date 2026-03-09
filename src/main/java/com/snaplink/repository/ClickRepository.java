package com.snaplink.repository;

import com.snaplink.model.entity.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    long countByUrlId(Long urlId);

    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM Click c WHERE c.urlId = :urlId")
    long countDistinctIpByUrlId(Long urlId);

    @Query("SELECT c.country, COUNT(c) FROM Click c WHERE c.urlId = :urlId AND c.country IS NOT NULL GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countByCountry(Long urlId);

    @Query("SELECT c.referrer, COUNT(c) FROM Click c WHERE c.urlId = :urlId GROUP BY c.referrer ORDER BY COUNT(c) DESC")
    List<Object[]> countByReferrer(Long urlId);

    @Query("SELECT c.deviceType, COUNT(c) FROM Click c WHERE c.urlId = :urlId AND c.deviceType IS NOT NULL GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> countByDeviceType(Long urlId);
}
