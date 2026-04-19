package com.snapfit.snapfitbackend.domain.auth.service;

import com.snapfit.snapfitbackend.domain.auth.dto.AuthResponse;
import com.snapfit.snapfitbackend.domain.auth.dto.ConsentUpdateRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.UserInfo;
import com.snapfit.snapfitbackend.domain.auth.entity.RefreshToken;
import com.snapfit.snapfitbackend.domain.auth.entity.UserEntity;
import com.snapfit.snapfitbackend.domain.auth.repository.RefreshTokenRepository;
import com.snapfit.snapfitbackend.domain.auth.repository.UserRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumMemberRepository;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import com.snapfit.snapfitbackend.domain.billing.repository.BillingOrderRepository;
import com.snapfit.snapfitbackend.domain.billing.repository.SubscriptionRepository;
import com.snapfit.snapfitbackend.domain.image.ImageStorageService;
import com.snapfit.snapfitbackend.domain.notification.repository.NotificationReadRepository;
import com.snapfit.snapfitbackend.domain.order.repository.OrderRepository;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateLikeRepository;
import com.snapfit.snapfitbackend.network.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final ImageStorageService imageStorageService;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final AlbumService albumService;
    private final AlbumRepository albumRepository;
    private final AlbumMemberRepository albumMemberRepository;
    private final OrderRepository orderRepository;
    private final BillingOrderRepository billingOrderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final TemplateLikeRepository templateLikeRepository;

    @Transactional
    public AuthResponse loginWithKakao(String accessToken) {
        // 1. 카카오 서버에 토큰 검증 및 사용자 정보 요청
        com.snapfit.snapfitbackend.domain.auth.dto.KakaoUserDto kakaoUser = getKakaoUserInfo(accessToken);

        // 2. 고유 ID 추출 (실제 카카오 회원 번호)
        Long kakaoId = kakaoUser.getId();
        String nickname = kakaoUser.getKakaoAccount().getProfile().getNickname();
        String profileImage = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();
        String email = kakaoUser.getKakaoAccount().getEmail(); // 동의 안 했으면 null일 수 있음

        return processLogin("KAKAO", kakaoId, accessToken, email, nickname, profileImage);
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        // 1. 구글 서버에 토큰 검증 및 사용자 정보 요청
        com.snapfit.snapfitbackend.domain.auth.dto.GoogleUserDto googleUser = getGoogleUserInfo(idToken);

        // 2. 고유 ID 추출 (sub Claim)
        // 구글 sub는 String이므로 Long으로 변환 가능한 해시값 사용하거나, DB User ID 체계를 String으로 변경 고려 필요.
        // 현재 DB ID가 Long이므로, sub의 hashCode를 사용하되 충돌 가능성 인지 필요 (실무에선 UUID 권장).
        // 여기선 기존 로직 유지(Long)를 위해 hashCode 사용.
        long userId = Math.abs(googleUser.getSub().hashCode());

        String email = googleUser.getEmail();
        String name = googleUser.getName();
        String picture = googleUser.getPicture();

        return processLogin("GOOGLE", userId, idToken, email, name, picture);
    }

    private com.snapfit.snapfitbackend.domain.auth.dto.KakaoUserDto getKakaoUserInfo(String accessToken) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        return restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                org.springframework.http.HttpMethod.GET,
                entity,
                com.snapfit.snapfitbackend.domain.auth.dto.KakaoUserDto.class).getBody();
    }

    private com.snapfit.snapfitbackend.domain.auth.dto.GoogleUserDto getGoogleUserInfo(String idToken) {
        // 구글은 id_token을 쿼리 파라미터로 전송
        return restTemplate.getForObject(
                "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                com.snapfit.snapfitbackend.domain.auth.dto.GoogleUserDto.class);
    }

    private AuthResponse processLogin(String provider, long userId, String providerToken, String email, String name,
            String profileImage) {
        UserEntity userEntity = userRepository.findById(userId).orElse(null);

        if (userEntity == null) {
            userEntity = UserEntity.builder()
                    .id(userId)
                    .email(email)
                    .name(name != null ? name : provider + "_USER_" + (userId % 1000))
                    .profileImageUrl(profileImage)
                    .provider(provider)
                    .build();
            userRepository.save(userEntity);
        } else {
            // [Optional] 로그인 시마다 정보 갱신 (선택 사항)
            boolean updated = false;
            if (email != null && !email.isBlank() && !email.equals(userEntity.getEmail())) {
                userEntity.setEmail(email);
                updated = true;
            }
            // 사용자가 앱에서 직접 변경한 프로필 이미지를 보존하기 위해,
            // 기존 프로필이 비어있을 때만 소셜 프로필 이미지로 초기화한다.
            if ((userEntity.getProfileImageUrl() == null || userEntity.getProfileImageUrl().isBlank())
                    && profileImage != null && !profileImage.isBlank()) {
                userEntity.setProfileImageUrl(profileImage);
                updated = true;
            }
            if (name != null && !name.equals(userEntity.getName())) {
                userEntity.setName(name);
                updated = true;
            }
            if (updated) {
                userRepository.save(userEntity);
            }
        }
        return generateAuthResponse(userEntity);
    }

    @Transactional
    public AuthResponse refresh(String requestRefreshToken) {
        // 1. 토큰 유효성 검증
        if (!jwtProvider.validateToken(requestRefreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 2. DB 존재 여부 확인
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // 3. 만료 확인 (JWT 자체 만료와 별개로 DB 만료일 체크)
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token expired");
        }

        // 4. 유저 확인
        UserEntity user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 5. 새 토큰 발급 및 기존 리프레시 토큰 삭제 (Rotation)
        refreshTokenRepository.delete(refreshToken);
        return generateAuthResponse(user);
    }

    @Transactional
    public UserInfo updateProfile(String token, String name, MultipartFile profileImage) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        String jwt = token.substring(7);
        if (!jwtProvider.validateToken(jwt)) {
            throw new IllegalArgumentException("Invalid access token");
        }

        String userIdStr = jwtProvider.getUserId(jwt);
        Long userId = Long.parseLong(userIdStr);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지가 있다면 삭제하는 로직도 추가 고려 가능 (ImageStorageService delete)
            try {
                String imageUrl = imageStorageService.upload(profileImage, "profiles");
                user.setProfileImageUrl(imageUrl);
            } catch (Exception e) {
                // 업로드 실패 시 500 에러를 명확히 하기 위해 로그 남기고 다시 던짐
                // (ControllerAdvice에서 500 처리됨)
                throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
            }
        }

        userRepository.save(user);

        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider())
                .build();
    }

    private AuthResponse generateAuthResponse(UserEntity user) {
        String accessToken = jwtProvider.createAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtProvider.createRefreshToken(String.valueOf(user.getId()));

        // [실서비스 최적화] 기존 리프레시 토큰 삭제 (단일 기기/세션 유지 정책)
        // 이를 통해 DB에 불필요한 토큰이 쌓이는 것을 방지하고, 보안성을 높임.
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.flush(); // 즉시 반영

        // 리프레시 토큰 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiryDate(LocalDateTime.now().plusNanos(jwtProvider.getRefreshTokenExpiration() * 1000000))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .user(userInfo)
                .build();
    }

    @Transactional
    public void deleteAccount(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        String jwt = authorization.substring(7);
        if (!jwtProvider.validateToken(jwt)) {
            throw new IllegalArgumentException("Invalid access token");
        }

        String userId = jwtProvider.getUserId(jwt);
        Long userIdLong = Long.parseLong(userId);
        UserEntity user = userRepository.findById(userIdLong).orElse(null);

        // 1) 소유 앨범 정리 (스토리지/DB 연쇄 삭제)
        var ownedAlbums = albumRepository.findByUserIdOrderByOrdersAscCreatedAtDesc(userId);
        for (var album : ownedAlbums) {
            try {
                albumService.deleteAlbum(album.getId(), userId);
            } catch (Exception ignored) {
                // 탈퇴는 계속 진행
            }
        }

        // 2) 공유 앨범 멤버십 제거
        albumMemberRepository.deleteByUserId(userId);

        // 3) 주문/구독/알림읽음/리프레시토큰/유저 제거
        orderRepository.deleteByUserId(userId);
        billingOrderRepository.deleteByUserId(userId);
        subscriptionRepository.deleteById(userId);
        templateLikeRepository.deleteByUserId(userId);
        notificationReadRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userIdLong);
        userRepository.deleteById(userIdLong);

        // 4) 프로필 이미지 정리
        if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            try {
                imageStorageService.delete(user.getProfileImageUrl());
            } catch (Exception ignored) {
                // 탈퇴는 계속 진행
            }
        }
    }

    @Transactional
    public void updateConsents(String authorization, ConsentUpdateRequest request) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        String jwt = authorization.substring(7);
        if (!jwtProvider.validateToken(jwt)) {
            throw new IllegalArgumentException("Invalid access token");
        }
        String userId = jwtProvider.getUserId(jwt);
        Long userIdLong = Long.parseLong(userId);
        UserEntity user = userRepository.findById(userIdLong)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request != null) {
            if (request.getTermsVersion() != null && !request.getTermsVersion().isBlank()) {
                user.setTermsVersion(request.getTermsVersion().trim());
            }
            if (request.getPrivacyVersion() != null && !request.getPrivacyVersion().isBlank()) {
                user.setPrivacyVersion(request.getPrivacyVersion().trim());
            }
            if (request.getMarketingOptIn() != null) {
                user.setMarketingOptIn(request.getMarketingOptIn());
            }
            if (request.getAgreedAt() != null && !request.getAgreedAt().isBlank()) {
                try {
                    user.setConsentedAt(OffsetDateTime.parse(request.getAgreedAt()).toLocalDateTime());
                } catch (Exception ignored) {
                    user.setConsentedAt(LocalDateTime.now());
                }
            } else if (user.getConsentedAt() == null) {
                user.setConsentedAt(LocalDateTime.now());
            }
        }
        userRepository.save(user);
    }
}
