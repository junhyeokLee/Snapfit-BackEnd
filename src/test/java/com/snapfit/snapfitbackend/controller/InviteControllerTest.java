package com.snapfit.snapfitbackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InviteController.class)
@AutoConfigureMockMvc(addFilters = false)
class InviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inviteRedirect_returnsHtmlWithDeeplink() throws Exception {
        mockMvc.perform(get("/invite")
                        .param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("snapfit://invite?token=abc123")));
    }

    @Test
    void invitePreview_returnsPng() throws Exception {
        mockMvc.perform(get("/invite/preview.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }
}
