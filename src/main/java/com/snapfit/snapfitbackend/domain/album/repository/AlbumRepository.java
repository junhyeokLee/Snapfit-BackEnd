package com.snapfit.snapfitbackend.domain.album.repository;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
}