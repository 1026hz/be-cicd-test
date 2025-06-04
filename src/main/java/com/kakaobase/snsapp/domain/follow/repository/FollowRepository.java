package com.kakaobase.snsapp.domain.follow.repository;


import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerUserAndFollowingUser(Member followerUser, Member followingUser);

    Optional<Follow> findByFollowerUserAndFollowingUser(Member followerUser, Member followingUser);
}
