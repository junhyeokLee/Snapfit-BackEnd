package com.snapfit.snapfitbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberRole;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberStatus;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InviteApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class InviteApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlbumService albumService;

    @Test
    void getInviteInfo_returnsInviteDetails() throws Exception {
        AlbumEntity album = AlbumEntity.builder()
                .id(3L)
                .ratio("3:4")
                .coverThumbnailUrl("thumb")
                .coverImageUrl("cover")
                .build();
        AlbumMemberEntity member = AlbumMemberEntity.builder()
                .album(album)
                .status(AlbumMemberStatus.PENDING)
                .build();

        when(albumService.getInvite("token-123")).thenReturn(member);

        mockMvc.perform(get("/api/invites/token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumId").value(3))
                .andExpect(jsonPath("$.ratio").value("3:4"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void acceptInvite_returnsMemberResponse() throws Exception {
        AlbumEntity album = AlbumEntity.builder()
                .id(5L)
                .build();
        AlbumMemberEntity member = AlbumMemberEntity.builder()
                .id(99L)
                .album(album)
                .userId("user-1")
                .role(AlbumMemberRole.EDITOR)
                .status(AlbumMemberStatus.ACCEPTED)
                .build();

        when(albumService.acceptInvite("token-1", "user-1")).thenReturn(member);

        mockMvc.perform(post("/api/invites/token-1/accept")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("userId", "user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.albumId").value(5))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }
}
