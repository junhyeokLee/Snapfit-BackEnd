package com.snapfit.snapfitbackend.domain.album.repository;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumMemberRepository extends JpaRepository<AlbumMemberEntity, Long> {

    Optional<AlbumMemberEntity> findByInviteToken(String inviteToken);

    List<AlbumMemberEntity> findByAlbumId(Long albumId);

    Optional<AlbumMemberEntity> findByAlbumIdAndUserId(Long albumId, String userId);

    boolean existsByAlbumIdAndUserIdAndStatus(Long albumId, String userId, AlbumMemberStatus status);
}
