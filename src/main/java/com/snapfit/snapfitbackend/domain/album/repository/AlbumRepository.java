package com.snapfit.snapfitbackend.domain.album.repository;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {

    /**
     * 특정 사용자가 저장한 앨범 목록 (최신순)
     */
    List<AlbumEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 소유자 + 멤버(수락됨) 포함 앨범 목록
     */
    @Query("""
        select distinct a
        from AlbumEntity a
        left join AlbumMemberEntity m
          on m.album = a and m.status = com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberStatus.ACCEPTED
        where a.userId = :userId or m.userId = :userId
        order by a.createdAt desc
        """)
    List<AlbumEntity> findAllAccessibleByUser(@Param("userId") String userId);

    /**
     * 커버 이미지 URL 사용 여부 확인용
     */
    boolean existsByCoverOriginalUrl(String coverOriginalUrl);
    boolean existsByCoverPreviewUrl(String coverPreviewUrl);
    boolean existsByCoverImageUrl(String coverImageUrl);
    boolean existsByCoverThumbnailUrl(String coverThumbnailUrl);
}