package com.snapfit.snapfitbackend.domain.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import com.snapfit.snapfitbackend.domain.billing.service.BillingService;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateLikeEntity;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateLikeRepository;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateLikeRepository templateLikeRepository;
    @Mock
    private AlbumService albumService;
    @Mock
    private BillingService billingService;

    private TemplateService templateService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        templateService = new TemplateService(
                templateRepository,
                templateLikeRepository,
                albumService,
                new ObjectMapper(),
                billingService
        );
    }

    @Test
    void likeTemplate_togglesLikeAndUpdatesCount() {
        TemplateEntity template = TemplateEntity.builder()
                .id(1L)
                .title("T1")
                .likeCount(2)
                .build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateLikeRepository.findByTemplateIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(TemplateLikeEntity.builder().templateId(1L).userId("user-1").build()));

        templateService.likeTemplate(1L, "user-1");

        assertEquals(1, template.getLikeCount());
        verify(templateLikeRepository).delete(any(TemplateLikeEntity.class));
        verify(templateRepository).save(template);
    }

    @Test
    void createAlbumFromTemplate_createsAlbumAndPages() {
        String json = """
            {
              "ratio": "3:4",
              "cover": { "theme": "classic", "layers": [{"id":"c1","type":"IMAGE"}] },
              "pages": [
                { "pageNumber": 1, "layers": [{"id":"p1","type":"TEXT"}] },
                { "pageNumber": 2, "layers": [{"id":"p2","type":"IMAGE"}] }
              ]
            }
            """;
        TemplateEntity template = TemplateEntity.builder()
                .id(5L)
                .title("Template A")
                .pageCount(2)
                .coverImageUrl("cover-url")
                .isPremium(true)
                .templateJson(json)
                .userCount(0)
                .build();

        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(billingService.hasActiveSubscription("user-1")).thenReturn(true);
        when(albumService.createAlbum(
                eq("user-1"),
                eq("3:4"),
                eq("Template A"),
                eq(2),
                any(String.class),
                eq("classic"),
                eq("cover-url"),
                eq("cover-url"),
                eq("cover-url"),
                eq("cover-url")
        )).thenReturn(AlbumEntity.builder().id(77L).build());

        templateService.createAlbumFromTemplate(5L, "user-1", null);

        verify(albumService).savePage(eq(77L), eq(1), any(String.class), eq(null), eq(null), eq("user-1"));
        verify(albumService).savePage(eq(77L), eq(2), any(String.class), eq(null), eq(null), eq("user-1"));

        ArgumentCaptor<TemplateEntity> captor = ArgumentCaptor.forClass(TemplateEntity.class);
        verify(templateRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getUserCount());
    }

    @Test
    void getAllTemplates_marksLikedWhenUserProvided() {
        TemplateEntity template = TemplateEntity.builder()
                .id(1L)
                .title("T")
                .pageCount(1)
                .likeCount(0)
                .userCount(0)
                .build();

        when(templateRepository.findAll()).thenReturn(List.of(template));
        when(templateLikeRepository.existsByTemplateIdAndUserId(1L, "user-1")).thenReturn(true);

        var responses = templateService.getAllTemplates("user-1");

        assertEquals(1, responses.size());
        assertEquals(true, responses.get(0).isLiked());
    }
}
