package com.kakaobase.snsapp.domain.comments.service;

import com.kakaobase.snsapp.domain.comments.converter.BotRecommentConverter;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRecommentService {

    private final RecommentRepository recommentRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final WebClient webClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Transactional
    public BotRecommentResponseDto handle(Post post, Comment comment) {
        // [1] 소셜봇 계정
        Member bot = memberRepository.findFirstByRole(Member.Role.BOT)
                .orElseThrow(() -> new IllegalStateException("소셜봇 계정이 없습니다."));

        // [2] 작성자 조회
        Member writer = memberRepository.findById(post.getMemberId())
                .orElseThrow(() -> new IllegalStateException("작성자 조회 실패"));

        // [3] 대댓글 리스트 조회
        List<Recomment> recomments = recommentRepository.findByCommentId(comment.getId());

        // [4] 요청 DTO 생성
        BotRecommentRequestDto requestDto = BotRecommentConverter.toRequestDto(post, writer, comment, recomments);

        // [5] FastAPI 호출
        String generatedContent = webClient.post()
                .uri(aiServerUrl + "/recomments/bot")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // [6] 대댓글 저장
        Recomment newRecomment = Recomment.builder()
                .comment(comment)
                .member(bot)
                .content(generatedContent)
                .build();
        recommentRepository.save(newRecomment);

        // [7] 댓글의 대댓글 수 증가
        comment.increaseRecommentCount();

        // [8] 응답 반환
        return BotRecommentConverter.toResponseDto(post, comment, bot, generatedContent);
    }

    @Async
    public void triggerAsync(Post post, Comment comment) {
        try {
            handle(post, comment);
            log.info("✅ 소셜봇 대댓글 트리거 완료 - postId: {}, commentId: {}", post.getId(), comment.getId());
        } catch (Exception e) {
            log.warn("⚠️ 소셜봇 대댓글 트리거 실패 - postId: {}, commentId: {}, reason: {}", post.getId(), comment.getId(), e.getMessage());
        }
    }
}
