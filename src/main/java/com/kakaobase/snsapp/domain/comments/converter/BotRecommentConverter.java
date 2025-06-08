package com.kakaobase.snsapp.domain.comments.converter;

import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.entity.Post;

import java.util.List;

public class BotRecommentConverter {

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
                post.getCreatedAt().toString(),
                post.getContent()
        );

        var recommentDtos = recomments.stream()
                .map(r -> new BotRecommentRequestDto.RecommentDto(
                        new BotRecommentRequestDto.UserDto(
                                r.getMember().getNickname(),
                                r.getMember().getClassName().toString()
                        ),
                        r.getCreatedAt().toString(),
                        r.getContent()
                ))
                .toList();

        var commentDto = new BotRecommentRequestDto.CommentDto(
                comment.getId(),
                new BotRecommentRequestDto.UserDto(
                        comment.getMember().getNickname(),
                        comment.getMember().getClassName().toString()
                ),
                comment.getCreatedAt().toString(),
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
}
