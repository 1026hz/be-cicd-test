package com.kakaobase.snsapp.domain.comments.dto;

import lombok.Builder;

@Builder
public record BotRecommentResponseDto(
        String board_type,
        Long post_id,
        Long comment_id,
        BotUserDto user,
        String content
) {
    public record BotUserDto(Long id, String nickname, String class_name) {}
}
