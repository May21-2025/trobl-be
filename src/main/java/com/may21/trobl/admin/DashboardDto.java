package com.may21.trobl.admin;

import com.may21.trobl.recordLimit.domain.RecordTrack;
import lombok.Getter;

import java.util.List;

public class DashboardDto {
    // ========== 대시보드 관련 DTO ==========

    @Getter
    public static class DashboardStats {
        // 기본 통계
        private final Long totalUsers;
        private final Long totalPosts;
        private final Long totalReports;
        private final Long activeUsers;

        // 사용자 세분화
        private final Long realUsers;        // 실제 사용자
        private final Long virtualUsers;     // 가상유저
        private final Long oAuthUsers;       // 소셜 로그인 사용자
        private final Long unverifiedUsers;  // 미인증 사용자

        // 게시글 상태
        private final Long reportedPosts;    // 신고된 게시글
        private final Long pendingPosts;     // 승인 대기 게시글

        // 상호작용
        private final Long totalComments;    // 총 댓글
        private final Long totalLikes;       // 총 좋아요
        private final Long totalViews;       // 총 조회수

        // 주간 변화
        private final Long newUsersThisWeek;
        private final Long newPostsThisWeek;

        public DashboardStats(Long totalUsers, Long totalPosts, Long totalReports, Long activeUsers,
                Long realUsers, Long virtualUsers, Long oAuthUsers, Long unverifiedUsers,
                Long reportedPosts, Long pendingPosts, Long totalComments, Long totalLikes, Long totalViews,
                Long newUsersThisWeek, Long newPostsThisWeek) {
            this.totalUsers = totalUsers;
            this.totalPosts = totalPosts;
            this.totalReports = totalReports;
            this.activeUsers = activeUsers;
            this.realUsers = realUsers;
            this.virtualUsers = virtualUsers;
            this.oAuthUsers = oAuthUsers;
            this.unverifiedUsers = unverifiedUsers;
            this.reportedPosts = reportedPosts;
            this.pendingPosts = pendingPosts;
            this.totalComments = totalComments;
            this.totalLikes = totalLikes;
            this.totalViews = totalViews;
            this.newUsersThisWeek = newUsersThisWeek;
            this.newPostsThisWeek = newPostsThisWeek;
        }
    }

    // ========== 그래프 데이터 관련 DTO ==========

    @Getter
    public static class UserGrowthData {
        private final String date;
        private final Long realUsers;
        private final Long virtualUsers;
        private final Long totalUsers;
        private final Long newRealUsers;
        private final Long newVirtualUsers;
        private final Long newTotalUsers;

        public UserGrowthData(String date, Long realUsers, Long virtualUsers, Long totalUsers,
                Long newRealUsers, Long newVirtualUsers, Long newTotalUsers) {
            this.date = date;
            this.realUsers = realUsers;
            this.virtualUsers = virtualUsers;
            this.totalUsers = totalUsers;
            this.newRealUsers = newRealUsers;
            this.newVirtualUsers = newVirtualUsers;
            this.newTotalUsers = newTotalUsers;
        }
    }

    @Getter
    public static class CommunityPostsData {
        private final String date;
        private final Long totalPosts;
        private final Long generalPosts;
        private final Long pollPosts;
        private final Long fairViewPosts;
        private final Long comments;
        private final Long likes;
        private final Long views;

        public CommunityPostsData(String date, Long totalPosts, Long generalPosts, Long pollPosts,
                Long fairViewPosts, Long comments, Long likes, Long views) {
            this.date = date;
            this.totalPosts = totalPosts;
            this.generalPosts = generalPosts;
            this.pollPosts = pollPosts;
            this.fairViewPosts = fairViewPosts;
            this.comments = comments;
            this.likes = likes;
            this.views = views;
        }
    }

    @Getter
    public static class RecordingAIStatsData {
        private final String date;
        private final Long recordings;
        private final Long aiReports;

        public RecordingAIStatsData(RecordTrack recordTrack) {
            this.date = recordTrack.getCreatedAt().toLocalDate().toString();
            this.recordings = 1L;
            this.aiReports = recordTrack.isAiGenerated() ? 1L : 0L;
        }

        public RecordingAIStatsData(String string, long updatedRecordings, long updatedAiReports) {
            this.date = string;
            this.recordings = updatedRecordings;
            this.aiReports = updatedAiReports;
        }
    }
    // 사용자 성장 데이터 DTO
    public record UserGrowthDataDto(List<UserGrowthData> userGrowthData) {}

    // 커뮤니티 게시글 데이터 DTO
    public record CommunityPostsDataDto(List<CommunityPostsData> communityPostsData) {}

    // 녹화 AI 통계 데이터 DTO
    public record RecordingAIStatsDataDto(
            List<RecordingAIStatsData> recordingAIStatsData) {}
}
