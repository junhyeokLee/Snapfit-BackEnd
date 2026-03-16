package com.snapfit.snapfitbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.dto.request.CreateAlbumRequest;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import com.snapfit.snapfitbackend.network.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlbumService albumService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void createAlbum_returnsCreatedResponse() throws Exception {
        CreateAlbumRequest request = new CreateAlbumRequest();
        request.setUserId("user-1");
        request.setRatio("3:4");
        request.setTitle("My Album");
        request.setCoverLayersJson("{\"layers\":[]}");

        AlbumEntity album = AlbumEntity.builder()
                .id(10L)
                .userId("user-1")
                .ratio("3:4")
                .title("My Album")
                .coverLayersJson("{\"layers\":[]}")
                .build();

        when(albumService.createAlbum(
                eq("user-1"),
                eq("3:4"),
                eq("My Album"),
                nullable(Integer.class),
                eq("{\"layers\":[]}"),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class)
        )).thenReturn(album);

        mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.albumId").value(10))
                .andExpect(jsonPath("$.ratio").value("3:4"))
                .andExpect(jsonPath("$.title").value("My Album"));
    }

    @Test
    void getMyAlbums_returnsAlbumList() throws Exception {
        AlbumEntity album = AlbumEntity.builder()
                .id(1L)
                .userId("user-1")
                .ratio("1:1")
                .title("Album A")
                .coverLayersJson("{}")
                .build();

        when(albumService.getAlbumsByUserId("user-1")).thenReturn(List.of(album));
        when(albumService.getAlbumLockerName(1L)).thenReturn("테스터");
        when(albumService.getAlbumLocker(1L)).thenReturn("7");

        mockMvc.perform(get("/api/albums")
                        .param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].albumId").value(1))
                .andExpect(jsonPath("$[0].title").value("Album A"))
                .andExpect(jsonPath("$[0].lockedBy").value("테스터"))
                .andExpect(jsonPath("$[0].lockedById").value("7"));
    }

    @Test
    void reorderAlbums_callsService() throws Exception {
        AlbumController.ReorderAlbumsRequest request = new AlbumController.ReorderAlbumsRequest();
        request.setAlbumIds(List.of(1L, 2L, 3L));

        mockMvc.perform(patch("/api/albums/reorder")
                        .param("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(albumService).reorderAlbums(eq("user-1"), eq(List.of(1L, 2L, 3L)));
    }

    @Test
    void lockAlbum_returnsConflictWhenAlreadyLocked() throws Exception {
        doThrow(new IllegalStateException("ALREADY_LOCKED"))
                .when(albumService).lockAlbum(eq(1L), eq("user-1"));

        mockMvc.perform(post("/api/albums/1/lock")
                        .param("userId", "user-1"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateAlbum_returnsUpdatedAlbum() throws Exception {
        CreateAlbumRequest request = new CreateAlbumRequest();
        request.setRatio("1:1");
        request.setTitle("Updated");

        AlbumEntity album = AlbumEntity.builder()
                .id(5L)
                .ratio("1:1")
                .title("Updated")
                .build();

        when(albumService.updateAlbum(
                eq(5L),
                eq("user-1"),
                eq("1:1"),
                nullable(Integer.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class)
        )).thenReturn(album);

        mockMvc.perform(put("/api/albums/5")
                        .param("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumId").value(5))
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deleteAlbum_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/albums/9")
                        .param("userId", "user-1"))
                .andExpect(status().isNoContent());

        verify(albumService).deleteAlbum(9L, "user-1");
    }
}
