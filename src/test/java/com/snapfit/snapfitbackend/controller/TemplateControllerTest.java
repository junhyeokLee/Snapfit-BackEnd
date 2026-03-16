package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.template.dto.response.TemplateResponse;
import com.snapfit.snapfitbackend.domain.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Test
    void getAllTemplates_returnsList() throws Exception {
        TemplateResponse response = TemplateResponse.builder()
                .id(1L)
                .title("Template A")
                .pageCount(12)
                .likeCount(3)
                .userCount(10)
                .build();

        when(templateService.getAllTemplates("user-1"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/templates")
                        .param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Template A"));
    }

    @Test
    void getTemplateDetail_returnsTemplate() throws Exception {
        TemplateResponse response = TemplateResponse.builder()
                .id(3L)
                .title("Detail")
                .pageCount(8)
                .likeCount(0)
                .userCount(1)
                .build();

        when(templateService.getTemplateDetail(3L, "user-2"))
                .thenReturn(response);

        mockMvc.perform(get("/api/templates/3")
                        .param("userId", "user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("Detail"));
    }

    @Test
    void likeTemplate_callsService() throws Exception {
        doNothing().when(templateService).likeTemplate(2L, "user-1");

        mockMvc.perform(post("/api/templates/2/like")
                        .param("userId", "user-1"))
                .andExpect(status().isOk());
    }

    @Test
    void createAlbumFromTemplate_returnsAlbum() throws Exception {
        AlbumEntity album = AlbumEntity.builder()
                .id(10L)
                .ratio("1:1")
                .title("Template Album")
                .build();

        when(templateService.createAlbumFromTemplate(5L, "user-1", null))
                .thenReturn(album);

        mockMvc.perform(post("/api/templates/5/use")
                        .param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ratio").value("1:1"))
                .andExpect(jsonPath("$.title").value("Template Album"));
    }
}
