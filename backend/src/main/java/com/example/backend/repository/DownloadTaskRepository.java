package com.example.backend.repository;

import com.example.backend.model.entity.DownloadTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadTaskRepository extends JpaRepository<DownloadTaskEntity, String> {
    
    List<DownloadTaskEntity> findByStatus(String status);
    
    List<DownloadTaskEntity> findByServiceName(String serviceName);
    
    @Query("SELECT COUNT(t) FROM DownloadTaskEntity t WHERE t.status = ?1")
    long countByStatus(String status);
    
    List<DownloadTaskEntity> findByStatusAndServiceName(String status, String serviceName);
}
