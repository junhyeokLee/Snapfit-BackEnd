package com.snapfit.snapfitbackend.domain.album.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberStatus;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumMemberRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumPageRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import com.snapfit.snapfitbackend.domain.auth.repository.UserRepository;
import com.snapfit.snapfitbackend.domain.image.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private AlbumPageRepository albumPageRepository;
    @Mock
    private AlbumMemberRepository albumMemberRepository;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AlbumLockService albumLockService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlbumService albumService;

    @Test
    void reorderAlbums_updatesOrdersForProvidedIds() {
        AlbumEntity album1 = AlbumEntity.builder().id(1L).userId("user-1").orders(5).build();
        AlbumEntity album2 = AlbumEntity.builder().id(2L).userId("user-1").orders(6).build();
        AlbumEntity album3 = AlbumEntity.builder().id(3L).userId("user-1").orders(7).build();

        when(albumRepository.findAllAccessibleByUser("user-1"))
                .thenReturn(List.of(album1, album2, album3));

        albumService.reorderAlbums("user-1", List.of(3L, 1L));

        assertEquals(0, album3.getOrders());
        assertEquals(1, album1.getOrders());
        assertEquals(6, album2.getOrders());
    }

    @Test
    void acceptInvite_setsMemberAcceptedAndClearsToken() {
        AlbumEntity album = AlbumEntity.builder().id(10L).userId("owner").build();
        AlbumMemberEntity invite = AlbumMemberEntity.builder()
                .album(album)
                .status(AlbumMemberStatus.PENDING)
                .inviteToken("token-1")
                .build();

        when(albumMemberRepository.findByInviteToken("token-1")).thenReturn(Optional.of(invite));
        when(albumMemberRepository.existsByAlbumIdAndUserIdAndStatus(
                10L, "user-1", AlbumMemberStatus.ACCEPTED)).thenReturn(false);
        when(albumMemberRepository.save(invite)).thenReturn(invite);

        AlbumMemberEntity result = albumService.acceptInvite("token-1", "user-1");

        assertEquals(AlbumMemberStatus.ACCEPTED, result.getStatus());
        assertEquals("user-1", result.getUserId());
        assertEquals(null, result.getInviteToken());
    }

    @Test
    void acceptInvite_marksAcceptedWhenAlreadyMember() {
        AlbumEntity album = AlbumEntity.builder().id(11L).userId("owner").build();
        AlbumMemberEntity invite = AlbumMemberEntity.builder()
                .album(album)
                .status(AlbumMemberStatus.PENDING)
                .inviteToken("token-2")
                .build();

        when(albumMemberRepository.findByInviteToken("token-2")).thenReturn(Optional.of(invite));
        when(albumMemberRepository.existsByAlbumIdAndUserIdAndStatus(
                11L, "user-2", AlbumMemberStatus.ACCEPTED)).thenReturn(true);
        when(albumMemberRepository.save(invite)).thenReturn(invite);

        AlbumMemberEntity result = albumService.acceptInvite("token-2", "user-2");

        assertEquals(AlbumMemberStatus.ACCEPTED, result.getStatus());
        assertEquals(null, result.getInviteToken());
    }

    @Test
    void getAlbumForMember_throwsWhenNotOwnerOrMember() {
        AlbumEntity album = AlbumEntity.builder().id(20L).userId("owner").build();

        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));
        when(albumMemberRepository.existsByAlbumIdAndUserIdAndStatus(
                20L, "user-3", AlbumMemberStatus.ACCEPTED)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> albumService.getAlbumForMember(20L, "user-3"));
    }

    @Test
    void createReadUpdateRead_flowWorksEndToEnd() {
        Map<Long, AlbumEntity> inMemory = new ConcurrentHashMap<>();
        AtomicLong seq = new AtomicLong(1L);

        when(albumRepository.save(any(AlbumEntity.class))).thenAnswer(invocation -> {
            AlbumEntity album = invocation.getArgument(0);
            if (album.getId() == null) {
                album.setId(seq.getAndIncrement());
            }
            inMemory.put(album.getId(), album);
            return album;
        });
        when(albumRepository.findById(any(Long.class)))
                .thenAnswer(invocation -> Optional.ofNullable(inMemory.get(invocation.getArgument(0))));
        when(albumRepository.findAllAccessibleByUser(anyString()))
                .thenAnswer(invocation -> inMemory.values().stream()
                        .filter(a -> invocation.getArgument(0).equals(a.getUserId()))
                        .toList());
        when(albumMemberRepository.save(any(AlbumMemberEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // create
        AlbumEntity created = albumService.createAlbum(
                "user-1",
                "3:4",
                "Trip",
                24,
                "{\"layers\":[]}",
                "classic",
                "orig-v1",
                "preview-v1",
                "preview-v1",
                "thumb-v1");

        assertNotNull(created.getId());
        assertEquals("Trip", created.getTitle());
        assertEquals(24, created.getTargetPages());

        // read
        List<AlbumEntity> albumsAfterCreate = albumService.getAlbumsByUserId("user-1");
        assertEquals(1, albumsAfterCreate.size());
        assertEquals(created.getId(), albumsAfterCreate.get(0).getId());
        assertEquals("preview-v1", albumsAfterCreate.get(0).getCoverPreviewUrl());

        // update
        AlbumEntity updated = albumService.updateAlbum(
                created.getId(),
                "user-1",
                "1:1",
                40,
                "{\"layers\":[{\"id\":\"cover-1\"}]}",
                "modern",
                "orig-v2",
                "preview-v2",
                "preview-v2",
                "thumb-v2");

        assertEquals("1:1", updated.getRatio());
        assertEquals(40, updated.getTargetPages());
        assertEquals("preview-v2", updated.getCoverPreviewUrl());
        assertEquals("thumb-v2", updated.getCoverThumbnailUrl());

        // read again
        AlbumEntity fetched = albumService.getAlbum(created.getId(), "user-1");
        assertEquals("1:1", fetched.getRatio());
        assertEquals(40, fetched.getTargetPages());
        assertEquals("{\"layers\":[{\"id\":\"cover-1\"}]}", fetched.getCoverLayersJson());
        assertEquals("modern", fetched.getCoverTheme());
    }
}
