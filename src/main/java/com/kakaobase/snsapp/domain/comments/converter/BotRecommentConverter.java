package com.kakaobase.snsapp.domain.comments.converter;

import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.entity.Post;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BotRecommentConverter {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static BotRecommentRequestDto toRequestDto(
            Post post,
            Member postWriter,
            Comment comment,
            List<Recomment> recomments
    ) {
        var postDto = new BotRecommentRequestDto.PostDto(
                post.getId(),
                new BotRecommentRequestDto.UserDto(
                        postWriter.getNickname(),
                        postWriter.getClassName().toString()
                ),
                formatUtc(post.getCreatedAt().toInstant(ZoneOffset.UTC)),
                post.getContent()
        );

        var recommentDtos = recomments.stream()
                .map(r -> new BotRecommentRequestDto.RecommentDto(
                        new BotRecommentRequestDto.UserDto(
                                r.getMember().getNickname(),
                                r.getMember().getClassName().toString()
                        ),
                        formatUtc(r.getCreatedAt().toInstant(ZoneOffset.UTC)),
                        r.getContent()
                ))
                .toList();

        var commentDto = new BotRecommentRequestDto.CommentDto(
                comment.getId(),
                new BotRecommentRequestDto.UserDto(
                        comment.getMember().getNickname(),
                        comment.getMember().getClassName().toString()
                ),
                formatUtc(comment.getCreatedAt().toInstant(ZoneOffset.UTC)),
                comment.getContent(),
                recommentDtos
        );

        return new BotRecommentRequestDto(
                post.getBoardType().name(),
                postDto,
                commentDto
        );
    }

    public static BotRecommentResponseDto toResponseDto(
            Post post, Comment comment, Member bot, String content
    ) {
        return BotRecommentResponseDto.builder()
                .board_type(post.getBoardType().name())
                .post_id(post.getId())
                .comment_id(comment.getId())
                .user(new BotRecommentResponseDto.BotUserDto(
                        bot.getId(),
                        bot.getNickname(),
                        bot.getClassName().toString()
                ))
                .content(content)
                .build();
    }

    private static String formatUtc(java.time.Instant instant) {
        return ISO_FORMATTER.format(instant); // ex: 2025-06-08T20:29:44.666858Z
    }
}
