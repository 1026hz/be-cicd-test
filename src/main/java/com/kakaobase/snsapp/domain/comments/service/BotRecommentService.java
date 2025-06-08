package com.kakaobase.snsapp.domain.comments.service;

import com.kakaobase.snsapp.domain.comments.converter.BotRecommentConverter;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.event.CommentCreatedEvent;
import com.kakaobase.snsapp.domain.comments.exception.CommentErrorCode;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.service.MemberService;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * 봇 대댓글 서비스
 *
 * <p>AI 서버와 통신하여 봇 게시글에 달린 댓글에 대한 대댓글을 생성합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BotRecommentService {

    private final CommentService commentService;
    private final MemberService memberService;
    private final WebClient webClient;

    // 대댓글 조회 시 사용할 최대 limit 값
    private static final int MAX_RECOMMENT_LIMIT = 100;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.server.timeout:30}")
    private int timeoutSeconds;

    @Value("${ai.server.retry.attempts:3}")
    private long retryAttempts;

    @Value("${ai.server.retry.delay:1}")
    private long retryDelaySeconds;

    /**
     * 봇 대댓글 생성
     *
     * @param event 댓글 생성 이벤트
     */
    @Transactional
    public void createBotRecomment(CommentCreatedEvent event) {
        log.info("봇 대댓글 생성 시작 - commentId: {}", event.getCommentId());

        try {
            // 1. 댓글 정보 조회
            Comment comment = commentService.findById(event.getCommentId());
            Post post = comment.getPost();

            // 2. 게시글 작성자 정보 조회
            BotRecommentRequestDto.UserInfo postAuthorInfo = memberService.getMemberBotInfo(post.getMember().getId());

            // 3. 대댓글 목록 조회 (큰 limit 사용)
            CommentRequestDto.RecommentPageRequest pageRequest =
                    new CommentRequestDto.RecommentPageRequest(MAX_RECOMMENT_LIMIT, null);

            CommentResponseDto.RecommentListResponse recommentResponse =
                    commentService.getRecommentsByCommentId(
                            BotConstants.BOT_MEMBER_ID,
                            comment.getId(),
                            pageRequest
                    );

            // 4. AI 요청 DTO 생성 (RecommentInfo 직접 사용)
            BotRecommentRequestDto.CreateRecommentRequest aiRequest =
                    BotRecommentConverter.toCreateRecommentRequest(
                            post,
                            postAuthorInfo,
                            comment,
                            recommentResponse.recomments()  // RecommentInfo 리스트 전달
                    );

            // 5. AI 서버 호출
            BotRecommentRequestDto.AiRecommentResponse aiResponse = callAiServerForRecommend(aiRequest);

            log.debug("AI 응답 수신: {}", BotRecommentConverter.toLogString(aiResponse));

            // 6. 응답 검증
            validateAiResponse(aiResponse);

            // 7. 대댓글 저장 (CommentService의 createComment 활용)
            Comment savedRecomment = saveRecomment(post.getId(), comment.getId(), aiResponse);

            log.info("봇 대댓글 생성 완료 - commentId: {}, content: {}...",
                    comment.getId(),
                    savedRecomment.getContent().substring(0, Math.min(20, savedRecomment.getContent().length())));

        } catch (CommentException e) {
            log.error("댓글 관련 오류 발생 - commentId: {}, error: {}",
                    event.getCommentId(), e.getMessage());
            throw e;
        } catch (WebClientResponseException e) {
            log.error("AI 서버 통신 오류 - commentId: {}, status: {}, body: {}",
                    event.getCommentId(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new CommentException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "AI 서버 통신 오류");
        } catch (Exception e) {
            log.error("봇 대댓글 생성 중 예상치 못한 오류 발생 - commentId: {}", event.getCommentId(), e);
            throw new CommentException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "봇 대댓글 생성 실패");
        }
    }

    /**
     * 댓글의 대댓글 목록 조회
     *
     * @param commentId 댓글 ID
     * @return 대댓글 목록
     */
    private List<Recomment> getRecommentsByCommentId(Long commentId) {
        // CommentService의 getRecommentsByCommentId 메서드를 활용
        // 하지만 해당 메서드는 페이징을 사용하므로 리포지토리 직접 호출이 더 효율적
        // CommentService에 전체 대댓글 조회 메서드가 없으므로 새로 만들어야 할 수도 있음

        // 임시로 빈 리스트 반환 (실제 구현 시 수정 필요)
        return List.of();
    }

    /**
     * AI 서버 호출
     *
     * @param request AI 요청 DTO
     * @return AI 응답 DTO
     */
    private BotRecommentRequestDto.AiRecommentResponse callAiServerForRecommend(
            BotRecommentRequestDto.CreateRecommentRequest request) {

        String url = aiServerUrl + "/recomments/bot";

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(BotRecommentRequestDto.ErrorResponse.class)
                                .flatMap(errorResponse -> {
                                    String errorMessage = BotRecommentConverter.getErrorMessage(errorResponse);
                                    log.error("AI 서버 클라이언트 오류: {}", errorMessage);
                                    return Mono.error(new CommentException(
                                            GeneralErrorCode.INTERNAL_SERVER_ERROR,
                                            errorMessage
                                    ));
                                })
                )
                .bodyToMono(BotRecommentRequestDto.AiRecommentResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(retryAttempts, Duration.ofSeconds(retryDelaySeconds))
                        .filter(throwable -> throwable instanceof WebClientResponseException &&
                                ((WebClientResponseException) throwable).getStatusCode().is5xxServerError())
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("AI 서버 재시도 횟수 초과");
                            return new CommentException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "AI 서버 응답 없음");
                        })
                )
                .block();
    }

    /**
     * AI 응답 검증
     *
     * @param response AI 응답
     * @throws CommentException 응답이 유효하지 않은 경우
     */
    private void validateAiResponse(BotRecommentRequestDto.AiRecommentResponse response) {
        if (!BotRecommentConverter.isValidResponse(response)) {
            log.error("유효하지 않은 AI 응답: {}", response);
            throw new CommentException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 AI 응답");
        }

        // 대댓글 내용 길이 검증
        String content = response.data().content();
        if (content.length() > 2000) {
            log.error("AI 응답 내용이 너무 깁니다: {} 자", content.length());
            throw new CommentException(CommentErrorCode.CONTENT_LENGTH_EXCEEDED);
        }
    }

    /**
     * 대댓글 저장
     *
     * @param postId 게시글 ID
     * @param commentId 댓글 ID
     * @param response AI 응답
     * @return 저장된 댓글(실제로는 대댓글) 엔티티
     */
    @Transactional
    protected Comment saveRecomment(Long postId, Long commentId,
                                    BotRecommentRequestDto.AiRecommentResponse response) {

        // AI 응답을 대댓글 생성 요청 DTO로 변환
        CommentRequestDto.CreateCommentRequest recommentRequest =
                BotRecommentConverter.toCreateCommentRequest(response, commentId);

        // CommentService의 createComment 메서드를 사용하여 대댓글 생성
        // parent_id가 있으므로 자동으로 대댓글로 처리됨
        CommentResponseDto.CreateCommentResponse recommentResponse =
                commentService.createComment(BotConstants.BOT_MEMBER_ID, postId, recommentRequest);

        // 생성된 대댓글 반환을 위해 조회
        // CreateCommentResponse에는 id가 있지만 content 전체를 위해 다시 조회
        return commentService.findById(recommentResponse.id());
    }
}