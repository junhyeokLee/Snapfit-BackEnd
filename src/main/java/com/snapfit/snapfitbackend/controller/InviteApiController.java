package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.album.dto.request.AcceptInviteRequest;
import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumMemberResponse;
import com.snapfit.snapfitbackend.domain.album.dto.response.InviteInfoResponse;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberEntity;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invite", description = "앨범 초대 API")
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteApiController {

    private final AlbumService albumService;

    @Operation(summary = "초대 링크 정보 조회")
    @GetMapping("/{token}")
    public ResponseEntity<InviteInfoResponse> getInviteInfo(@PathVariable String token) {
        AlbumMemberEntity invite = albumService.getInvite(token);
        AlbumEntity album = invite.getAlbum();
        InviteInfoResponse response = InviteInfoResponse.builder()
                .albumId(album.getId())
                .ratio(album.getRatio())
                .coverThumbnailUrl(album.getCoverThumbnailUrl())
                .coverImageUrl(album.getCoverImageUrl())
                .status(invite.getStatus().name())
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "초대 수락")
    @PostMapping("/{token}/accept")
    public ResponseEntity<AlbumMemberResponse> acceptInvite(
            @PathVariable String token,
            @RequestBody AcceptInviteRequest request
    ) {
        AlbumMemberEntity member = albumService.acceptInvite(token, request.getUserId());
        AlbumMemberResponse response = AlbumMemberResponse.builder()
                .id(member.getId())
                .albumId(member.getAlbum().getId())
                .userId(member.getUserId())
                .role(member.getRole().name())
                .status(member.getStatus().name())
                .build();
        return ResponseEntity.ok(response);
    }
}
