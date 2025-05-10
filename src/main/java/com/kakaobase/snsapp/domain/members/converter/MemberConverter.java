package com.kakaobase.snsapp.domain.members.converter;

import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Member 도메인의 객체 변환을 담당하는 컨버터입니다.
 * DTO와 Entity 사이의 변환을 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class MemberConverter {

    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 요청 DTO를 Member 엔티티로 변환합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 생성된 Member 엔티티
     */
    public Member toEntity(MemberRequestDto.SignUp request) {
        // 기수명 변환
        Member.ClassName className;
        try {
            className = Member.ClassName.valueOf(request.class_name());
        } catch (IllegalArgumentException e) {
            throw new CustomException(GeneralErrorCode.RESOURCE_NOT_FOUND, "class_name");
        }

        // 회원 엔티티 생성 및 반환
        return Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickname(request.nickname())
                .className(className)
                .githubUrl(request.github_url())
                .role(Member.Role.USER)
                .isBanned(false)
                .followerCount(0)
                .followingCount(0)
                .build();

    }

    /**
     * Member 엔티티를 프로필 응답 DTO로 변환합니다.
     *
     * @param member Member 엔티티
     * @param isMe 본인 여부
     * @param isFollowing 팔로우 여부
     * @return 프로필 응답 DTO
     */
    public MemberResponseDto.Profile toProfileResponseDto(Member member, boolean isMe, boolean isFollowing) {
        return new MemberResponseDto.Profile(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getClassName(),
                member.getProfileImgUrl(),
                member.getGithubUrl(),
                member.getFollowerCount(),
                member.getFollowingCount(),
                isMe,
                isFollowing
        );
    }
}