package com.kakaobase.snsapp.domain.follow.service;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.follow.converter.FollowConverter;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.exception.FollowErrorCode;
import com.kakaobase.snsapp.domain.follow.exception.FollowException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowConverter followConverter;
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;

    @Transactional
    public void addFollowing(Long targetUserId, CustomUserDetails userDetails) {



        //팔로잉 신청한 사람
        Member followerUser = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        //팔로잉 요청 받은 사람
        Member followingUser = memberRepository.findById(targetUserId)
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "targetUserId"));

        if(followRepository.existsByFollowerUserAndFollowingUser(followerUser, followingUser)){
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWING);
        }

        followerUser.incrementFollowingCount();
        followingUser.incrementFollowerCount();

        Follow follow = followConverter.toFollowEntity(followerUser, followingUser);
        followRepository.save(follow);
    }

    @Transactional
    public void removeFollowing(Long targetUserId, CustomUserDetails userDetails){

        //팔로잉 신청한 사람
        Member followerUser = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));
        //팔로잉 요청 받은 사람
        Member followingUser = memberRepository.findById(targetUserId)
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "targetUserId"));

        Follow follow = followRepository.findByFollowerUserAndFollowingUser(followerUser, followingUser)
                .orElseThrow(()-> new FollowException(FollowErrorCode.ALREADY_UNFOLLOWING));

        followerUser.decrementFollowingCount();
        followingUser.decrementFollowerCount();

        followRepository.delete(follow);
    }
}
