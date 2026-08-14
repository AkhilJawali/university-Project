package com.utms.masterdata.room;

import com.utms.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/blocks")
@RequiredArgsConstructor
@Tag(name = "Resource Blocks", description = "Room resource block management")
public class ResourceBlockController {

    private final ResourceBlockService resourceBlockService;

    @GetMapping
    @Operation(summary = "List resource blocks for a room")
    public ResponseEntity<PagedResponse<ResourceBlockDto>> list(
            @PathVariable Long roomId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(resourceBlockService.findAllByRoomId(roomId, pageable)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'HOD')")
    @Operation(summary = "Create a new resource block request")
    public ResponseEntity<ResourceBlockDto> create(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateResourceBlockRequest request,
            Authentication authentication) {
        String requestedBy = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceBlockService.create(roomId, request, requestedBy));
    }

    @PutMapping("/{blockId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'HOD')")
    @Operation(summary = "Approve a resource block request")
    public ResponseEntity<ResourceBlockDto> approve(
            @PathVariable Long roomId,
            @PathVariable Long blockId,
            Authentication authentication) {
        String approvedBy = authentication.getName();
        return ResponseEntity.ok(resourceBlockService.approve(roomId, blockId, approvedBy));
    }

    @PutMapping("/{blockId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Activate an approved resource block")
    public ResponseEntity<ResourceBlockDto> activate(
            @PathVariable Long roomId,
            @PathVariable Long blockId) {
        return ResponseEntity.ok(resourceBlockService.activate(roomId, blockId));
    }

    @PutMapping("/{blockId}/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'HOD', 'COORDINATOR')")
    @Operation(summary = "Release an active or approved resource block")
    public ResponseEntity<ResourceBlockDto> release(
            @PathVariable Long roomId,
            @PathVariable Long blockId) {
        return ResponseEntity.ok(resourceBlockService.release(roomId, blockId));
    }
}
