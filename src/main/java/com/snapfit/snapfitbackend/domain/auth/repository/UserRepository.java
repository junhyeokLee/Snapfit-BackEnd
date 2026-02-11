package com.snapfit.snapfitbackend.domain.auth.repository;

import com.snapfit.snapfitbackend.domain.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
