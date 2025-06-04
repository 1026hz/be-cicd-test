package com.kakaobase.snsapp.domain.follow.service;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.exception.FollowErrorCode;
import com.kakaobase.snsapp.domain.follow.exception.FollowException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final EntityManager entityManager;

    public void addFollowing(Long targetUserId, CustomUserDetails userDetails) {

        //팔로잉 신청한 사람
        Member followerProxy = entityManager.getReference(Member.class, Long.valueOf(userDetails.getId()));
        //팔로잉 요청 받은 사람
        Member followingProxy = entityManager.getReference(Member.class, targetUserId);

        if(followRepository.existsByFollowerUserAndFollowingUser(followerProxy, followingProxy)){
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWING);
        }
        //Converter로 follower생성
        followRepository.save();


    }

    public void removeFollowing(){

    }
}
