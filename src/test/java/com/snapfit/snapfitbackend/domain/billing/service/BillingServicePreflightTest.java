package com.snapfit.snapfitbackend.domain.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumPageRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import com.snapfit.snapfitbackend.domain.billing.dto.StoragePreflightResponse;
import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionEntity;
import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionStatus;
import com.snapfit.snapfitbackend.domain.billing.repository.BillingOrderRepository;
import com.snapfit.snapfitbackend.domain.billing.repository.SubscriptionRepository;
import com.snapfit.snapfitbackend.domain.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServicePreflightTest {

    @Mock
    private BillingOrderRepository billingOrderRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private AlbumPageRepository albumPageRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BillingService billingService;

    @Test
    void preflight_deniesWhenProjectedExceedsHardLimit() {
        ReflectionTestUtils.setField(billingService, "freeSoftLimitBytes", 3_000_000L);
        ReflectionTestUtils.setField(billingService, "freeHardLimitBytes", 3_000_000L);
        ReflectionTestUtils.setField(billingService, "proSoftLimitBytes", 10_000_000L);
        ReflectionTestUtils.setField(billingService, "proHardLimitBytes", 10_000_000L);

        final AlbumEntity album = AlbumEntity.builder()
                .id(1L)
                .userId("user-1")
                .coverOriginalUrl("orig")
                .coverPreviewUrl("preview")
                .coverThumbnailUrl("thumb")
                .build();

        when(subscriptionRepository.findById("user-1")).thenReturn(Optional.empty());
        when(albumRepository.findByUserIdOrderByOrdersAscCreatedAtDesc("user-1"))
                .thenReturn(List.of(album));
        when(albumPageRepository.findByAlbumId(1L)).thenReturn(List.of());

        StoragePreflightResponse result = billingService.preflightStorage("user-1", 300_000L);

        assertFalse(result.isAllowed());
        assertEquals("HARD_LIMIT_EXCEEDED", result.getReason());
        assertEquals(BillingService.PLAN_FREE, result.getPlanCode());
    }

    @Test
    void preflight_usesProLimitForActiveSubscription() {
        ReflectionTestUtils.setField(billingService, "freeSoftLimitBytes", 1_000_000L);
        ReflectionTestUtils.setField(billingService, "freeHardLimitBytes", 1_000_000L);
        ReflectionTestUtils.setField(billingService, "proSoftLimitBytes", 10_000_000L);
        ReflectionTestUtils.setField(billingService, "proHardLimitBytes", 10_000_000L);

        final SubscriptionEntity activeSub = SubscriptionEntity.builder()
                .userId("user-1")
                .planCode(BillingService.PLAN_PRO_MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .build();
        final AlbumEntity album = AlbumEntity.builder()
                .id(1L)
                .userId("user-1")
                .coverOriginalUrl("orig")
                .coverPreviewUrl("preview")
                .coverThumbnailUrl("thumb")
                .build();

        when(subscriptionRepository.findById("user-1")).thenReturn(Optional.of(activeSub));
        when(albumRepository.findByUserIdOrderByOrdersAscCreatedAtDesc("user-1"))
                .thenReturn(List.of(album));
        when(albumPageRepository.findByAlbumId(1L)).thenReturn(List.of());

        StoragePreflightResponse result = billingService.preflightStorage("user-1", 300_000L);

        assertTrue(result.isAllowed());
        assertEquals("OK", result.getReason());
        assertEquals(BillingService.PLAN_PRO_MONTHLY, result.getPlanCode());
    }
}
