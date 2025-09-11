package com.may21.trobl.admin.repository;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.admin.domain.PostDetailInfo;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostDetailInfoRepository extends JpaRepository<PostDetailInfo, Long> {

    List<PostDetailInfo> findAllByPostIdIn(List<Long> oldPostIds);

    @Query("SELECT p FROM PostDetailInfo p WHERE p.postType IN :postingTypes")
    Page<PostDetailInfo> findAllContainsTagsFilteredByTypes(List<PostingType> postingTypes,
            Pageable pageable);

    // TOTAL_ENGAGEMENT: 좋아요 + 댓글 + 조회수 + 투표수의 합계로 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY (p.likeCount + p.commentCount + p" +
            ".viewCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByTotalEngagementDesc(PostingType announcementType, int count);

    // VIEW_COUNT: 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY p.viewCount DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByViewCountDesc(PostingType announcementType, int count);

    // LIKE_COUNT: 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY p.likeCount DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByLikeCountDesc(PostingType announcementType, int count);

    // PARTICIPANT_COUNT: 참여자 수 (댓글 + 투표로 가정)
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY (p.commentCount + p.voteCount) DESC " +
            "LIMIT :count")
    List<Long> findAllPostIdsOrderByParticipantCountDesc(PostingType announcementType, int count);

    // LIKE_COMMENT_COUNT: 좋아요 + 댓글 수
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY (p.likeCount + p.commentCount) DESC " +
            "LIMIT :count")
    List<Long> findAllPostIdsOrderByLikeCommentCountDesc(PostingType announcementType, int count);

    // TOTAL_ENGAGEMENT: 특정 날짜 이후, 총 참여도 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByTotalEngagementDesc(
            @Param("fromDate") LocalDateTime fromDate, PostingType announcementType, int count);


    // VIEW_COUNT: 특정 날짜 이후, 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.createdAt >= :fromDate ORDER BY p" +
            ".viewCount DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByViewCountDesc(
            @Param("fromDate") LocalDateTime fromDate, PostingType announcementType, int count);

    // LIKE_COUNT: 특정 날짜 이후, 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.createdAt >= :fromDate ORDER BY p" +
            ".likeCount DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByLikeCountDesc(
            @Param("fromDate") LocalDateTime fromDate, PostingType announcementType, int count);

    // PARTICIPANT_COUNT: 특정 날짜 이후, 참여자 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.createdAt >= :fromDate " +
            "ORDER BY (p.commentCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByParticipantCountDesc(
            @Param("fromDate") LocalDateTime fromDate, PostingType announcementType, int count);

    // LIKE_COMMENT_COUNT: 특정 날짜 이후, 좋아요 + 댓글 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByLikeCommentCountDesc(
            @Param("fromDate") LocalDateTime fromDate, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByTotalEngagementDesc(@Param("address") String address,
            PostingType announcementType, int count);

    // VIEW_COUNT: 특정 주소, 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address ORDER BY p.viewCount" +
            " DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByViewCountDesc(@Param("address") String address,
            PostingType announcementType, int count);

    // LIKE_COUNT: 특정 주소, 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address ORDER BY p.likeCount" +
            " DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByLikeCountDesc(@Param("address") String address,
            PostingType announcementType, int count);

    // PARTICIPANT_COUNT: 특정 주소, 참여자 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address " +
            "ORDER BY (p.commentCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByParticipantCountDesc(@Param("address") String address,
            PostingType announcementType, int count);

    // LIKE_COMMENT_COUNT: 특정 주소, 좋아요 + 댓글 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address " +
            "ORDER BY (p.likeCount + p.commentCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByLikeCommentCountDesc(@Param("address") String address,
            PostingType announcementType, int count);


    // 주소 + 시간 조건이 모두 있는 경우

    // TOTAL_ENGAGEMENT: 특정 주소 + 특정 날짜 이후, 총 참여도 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByTotalEngagementDesc(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            PostingType announcementType, int count);


    // VIEW_COUNT: 특정 주소 + 특정 날짜 이후, 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY p.viewCount DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByViewCountDesc(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            PostingType announcementType, int count);

    // LIKE_COUNT: 특정 주소 + 특정 날짜 이후, 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY p.likeCount DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByLikeCountDesc(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            PostingType announcementType, int count);

    // PARTICIPANT_COUNT: 특정 주소 + 특정 날짜 이후, 참여자 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY (p.commentCount + p.voteCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByParticipantCountDesc(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            PostingType announcementType, int count);

    // LIKE_COMMENT_COUNT: 특정 주소 + 특정 날짜 이후, 좋아요 + 댓글 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount) DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByLikeCommentCountDesc(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            PostingType announcementType, int count);

    // TOTAL_ENGAGEMENT: 특정 postId 집합 안에서 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p" +
            ".postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByTotalEngagementDescInPostIds(
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.postId IN :postIds " +
            "ORDER BY p.viewCount  DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByViewCountDescInPostIds(@Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.postId IN :postIds " +
            "ORDER BY p.likeCount  DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByLikeCountDescInPostIds(@Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p.postId IN :postIds " +
            "ORDER BY (p.commentCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByParticipantCountDescInPostIds(
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p" +
            ".postId IN :postIds " + "ORDER BY (p.likeCount + p.commentCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsOrderByLikeCommentCountDescInPostIds(
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p" +
            ".createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByTotalEngagementDescInPostIds(
            @Param("fromDate") LocalDateTime fromDate, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE  p.postType != :announcementType AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY p.viewCount  DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByViewCountDescInPostIds(
            @Param("fromDate") LocalDateTime fromDate, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE  p.postType != :announcementType AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY p.likeCount  DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByLikeCountDescInPostIds(
            @Param("fromDate") LocalDateTime fromDate, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY (p.commentCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByParticipantCountDescInPostIds(
            @Param("fromDate") LocalDateTime fromDate, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByLikeCommentCountDescInPostIds(
            @Param("fromDate") LocalDateTime fromDate, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByTotalEngagementDescInPostIds(
            @Param("address") String address, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.postId IN :postIds " +
            "ORDER BY p.viewCount  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByViewCountDescInPostIds(
            @Param("address") String address, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.postId IN :postIds " +
            "ORDER BY p.likeCount  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByLikeCountDescInPostIds(
            @Param("address") String address, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.postId IN :postIds " +
            "ORDER BY (p.commentCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByParticipantCountDescInPostIds(
            @Param("address") String address, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressOrderByLikeCommentCountDescInPostIds(
            @Param("address") String address, @Param("postIds") List<Long> postIds,
            PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByTotalEngagementDescInPostIds(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY p.viewCount  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByViewCountDescInPostIds(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY p.likeCount  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByLikeCountDescInPostIds(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType  AND p.address = :address AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY (p.commentCount + p.voteCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByParticipantCountDescInPostIds(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.postType != :announcementType AND p" +
            ".address = :address AND p.createdAt >= :fromDate AND p.postId IN :postIds " +
            "ORDER BY (p.likeCount + p.commentCount)  DESC LIMIT :count")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByLikeCommentCountDescInPostIds(
            @Param("address") String address, @Param("fromDate") LocalDateTime fromDate,
            @Param("postIds") List<Long> postIds, PostingType announcementType, int count);

}
