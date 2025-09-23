package com.may21.trobl.admin.service;

import com.may21.trobl._global.enums.DateType;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.admin.DashboardDto;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.post.domain.PostLikeRepository;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.PostViewRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.recordLimit.domain.RecordTrack;
import com.may21.trobl.recordLimit.repository.RecordTrackRepository;
import com.may21.trobl.report.ReportRepository;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final PostViewRepository postViewRepository;
    private final PostLikeRepository postLikeRepository;
    private final RecordTrackRepository recordTrackRepository;

    @Transactional(readOnly = true)
    public DashboardDto.DashboardStats getDashboardStats() {
        // 기본 통계
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalReports = reportRepository.count();

        // 활성 사용자 수 (최근 30일 내 로그인)
        LocalDate thirtyDaysAgo = LocalDate.from(LocalDateTime.now()
                .minusDays(30));
        long activeUsers = userRepository.countByLastLoginDateAfter(thirtyDaysAgo);

        // 사용자 세분화
        long virtualUsers = userRepository.findByTestUserIsTrue(Pageable.unpaged())
                .getTotalElements();
        long realUsers =
                userRepository.findByTestUserIsFalseAndUnregisteredIsFalse(Pageable.unpaged())
                        .getTotalElements();
        long oAuthUsers = userRepository.findAllOAuth()
                .size();
        long unverifiedUsers = userRepository.countByUnregistered(true);

        // 게시글 상태
        long reportedPosts = postRepository.findAllByReportedTrue()
                .size();
        long pendingPosts = postRepository.findAllUnconfirmedPostsByUserIdIn(
                        userRepository.findAll()
                                .stream()
                                .map(User::getId)
                                .toList(), Pageable.unpaged())
                .getTotalElements();

        // 상호작용
        long totalComments = commentRepository.count();
        long totalLikes = postRepository.findAll()
                .stream()
                .mapToLong(posting -> posting.getPostLikes() != null ? posting.getPostLikes()
                        .size() : 0)
                .sum();
        long views = postViewRepository.count();
        long totalViews = postRepository.findAll()
                .stream()
                .mapToLong(Posting::getViewCount)
                .sum();
        totalViews += views;

        // 주간 변화
        LocalDateTime weekAgo = LocalDateTime.now()
                .minusWeeks(1);
        LocalDate weekAgoDate = LocalDate.from(weekAgo);
        long newUsersThisWeek = userRepository.countBySignUpDateAfter(weekAgoDate);
        long newPostsThisWeek =
                postRepository.countByCreatedAtAfter(weekAgo, PostingType.ANNOUNCEMENT);

        return new DashboardDto.DashboardStats(totalUsers, totalPosts, totalReports, activeUsers,
                realUsers, virtualUsers, oAuthUsers, unverifiedUsers, reportedPosts, pendingPosts,
                totalComments, totalLikes, totalViews, newUsersThisWeek, newPostsThisWeek);
    }

    @Transactional(readOnly = true)
    public DashboardDto.UserGrowthDataDto getUserGrowthData(DateType dateType) {
        List<DashboardDto.UserGrowthData> userGrowthData = new ArrayList<>();

        switch (dateType) {
            case DateType.WEEK:
                for (int i = 7; i >= 0; i--) {
                    LocalDate weekStart = LocalDate.now()
                            .minusWeeks(i)
                            .with(DayOfWeek.MONDAY);
                    LocalDate weekEnd = weekStart.plusDays(6);
                    String dateStr = weekStart.toString();
                    userGrowthData.add(createUserGrowthDataForWeek(weekStart, weekEnd, dateStr));
                }
                break;
            case DateType.MONTH:
                for (int i = 5; i >= 0; i--) {
                    LocalDate monthStart = LocalDate.now()
                            .minusMonths(i)
                            .withDayOfMonth(1);
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    String dateStr = monthStart.toString();
                    userGrowthData.add(createUserGrowthDataForMonth(monthStart, monthEnd, dateStr));
                }
                break;
            case DateType.DAY:
            default:
                LocalDate defaultStartDate = LocalDate.now()
                        .minusDays(6);
                LocalDate defaultEndDate = LocalDate.now();
                Map<LocalDate, DashboardDto.UserGrowthData> defaultUserStatsMap =
                        getUserGrowthDataBatch(defaultStartDate, defaultEndDate);
                for (int i = 6; i >= 0; i--) {
                    LocalDate date = LocalDate.now()
                            .minusDays(i);
                    String dateStr = date.toString();
                    userGrowthData.add(defaultUserStatsMap.getOrDefault(date,
                            new DashboardDto.UserGrowthData(dateStr, 0L, 0L, 0L, 0L, 0L, 0L)));
                }
                break;
        }

        return new DashboardDto.UserGrowthDataDto(userGrowthData);
    }

    @Transactional(readOnly = true)
    public DashboardDto.CommunityPostsDataDto getCommunityPostsData(DateType dateType) {
        List<DashboardDto.CommunityPostsData> communityPostsData = new ArrayList<>();

        switch (dateType) {

            case DateType.WEEK:
                for (int i = 7; i >= 0; i--) {
                    LocalDate weekStart = LocalDate.now()
                            .minusWeeks(i)
                            .with(java.time.DayOfWeek.MONDAY);
                    LocalDate weekEnd = weekStart.plusDays(6);
                    communityPostsData.add(createCommunityPostsDataForWeek(weekStart, weekEnd));
                }
                break;
            case DateType.MONTH:
                for (int i = 5; i >= 0; i--) {
                    LocalDate monthStart = LocalDate.now()
                            .minusMonths(i)
                            .withDayOfMonth(1);
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    communityPostsData.add(createCommunityPostsDataForMonth(monthStart, monthEnd));
                }
                break;
            case DateType.DAY:
            default:
                LocalDate defaultStartDate = LocalDate.now()
                        .minusDays(6);
                LocalDate defaultEndDate = LocalDate.now();
                LocalDateTime defaultStartDateTime = defaultStartDate.atStartOfDay();
                LocalDateTime defaultEndDateTime = defaultEndDate.plusDays(1)
                        .atStartOfDay();
                Map<LocalDate, DashboardDto.CommunityPostsData> defaultPostStatsMap =
                        getCommunityPostsDataBatch(defaultStartDateTime, defaultEndDateTime);

                for (int i = 6; i >= 0; i--) {
                    LocalDate date = LocalDate.now()
                            .minusDays(i);
                    String dateStr = date.toString();
                    communityPostsData.add(defaultPostStatsMap.getOrDefault(date,
                            new DashboardDto.CommunityPostsData(dateStr, 0L, 0L, 0L, 0L, 0L, 0L,
                                    0L)));
                }
                break;
        }

        return new DashboardDto.CommunityPostsDataDto(communityPostsData);
    }

    @Transactional(readOnly = true)
    public DashboardDto.RecordingAIStatsDataDto getRecordingAIStatsData(DateType dateType) {
        switch (dateType) {
            case DateType.WEEK:
                LocalDate weekStart = LocalDate.now()
                        .minusWeeks(6)
                        .with(DayOfWeek.MONDAY);
                LocalDate weekEnd = LocalDate.now();
                return createRecordingAIStatsDataForWeek(weekStart, weekEnd);
            case DateType.MONTH:
                LocalDate monthStart = LocalDate.now()
                        .minusMonths(6)
                        .withDayOfMonth(1);
                LocalDate monthEnd = LocalDate.now();
                return createRecordingAIStatsDataForMonth(monthStart, monthEnd);
            case DateType.DAY:
            default:
                LocalDate defaultStartDate = LocalDate.now()
                        .minusDays(6);
                LocalDate defaultEndDate = LocalDate.now();
                LocalDateTime defaultStartDateTime = defaultStartDate.atStartOfDay();
                LocalDateTime defaultEndDateTime = defaultEndDate.plusDays(1)
                        .atStartOfDay();
                return getRecordingAIStatsDataBatch(defaultStartDateTime, defaultEndDateTime);
        }
    }

    // Helper methods for data generation
    private Map<LocalDate, DashboardDto.UserGrowthData> getUserGrowthDataBatch(LocalDate startDate,
            LocalDate endDate) {
        Map<LocalDate, DashboardDto.UserGrowthData> resultMap = new HashMap<>();

        // 전체 사용자 수 조회 (한 번만)
        long totalRealUsers =
                userRepository.findByTestUserIsFalseAndUnregisteredIsFalse(Pageable.unpaged())
                        .getTotalElements();
        long totalVirtualUsers = userRepository.findByTestUserIsTrue(Pageable.unpaged())
                .getTotalElements();
        long totalUsers = totalRealUsers + totalVirtualUsers;

        // 일별 신규 사용자 통계 배치 조회
        List<Object[]> dailyStats =
                userRepository.getDailyUserStatsBetween(startDate, endDate.plusDays(1));

        for (Object[] stat : dailyStats) {
            LocalDate date = ((java.sql.Date) stat[0]).toLocalDate();
            Long realUsers = ((Number) stat[1]).longValue();
            Long virtualUsers = ((Number) stat[2]).longValue();
            Long newTotalUsers = realUsers + virtualUsers;

            resultMap.put(date, new DashboardDto.UserGrowthData(date.toString(), totalRealUsers,
                    totalVirtualUsers, totalUsers, realUsers, virtualUsers, newTotalUsers));
        }

        return resultMap;
    }

    private Map<LocalDate, DashboardDto.CommunityPostsData> getCommunityPostsDataBatch(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<LocalDate, DashboardDto.CommunityPostsData> resultMap = new HashMap<>();

        // 일별 포스트 통계 배치 조회
        List<Object[]> postStats =
                postRepository.getDailyPostStatsBetween(startDateTime, endDateTime);
        Map<LocalDate, Long> totalPostsMap = postStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        // 일별 일반 포스트 통계
        List<Object[]> generalPostStats =
                postRepository.getDailyPostStatsBetween(startDateTime, endDateTime,
                        PostingType.GENERAL);
        Map<LocalDate, Long> generalPostsMap = generalPostStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        // 일별 투표 포스트 통계
        List<Object[]> pollPostStats =
                postRepository.getDailyPostStatsBetween(startDateTime, endDateTime,
                        PostingType.POLL);
        Map<LocalDate, Long> pollPostsMap = pollPostStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        // 일별 공정시청 포스트 통계
        List<Object[]> fairViewPostStats =
                postRepository.getDailyPostStatsBetween(startDateTime, endDateTime,
                        PostingType.FAIR_VIEW);
        Map<LocalDate, Long> fairViewPostsMap = fairViewPostStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        // 일별 댓글 통계 배치 조회
        List<Object[]> commentStats =
                commentRepository.getDailyCommentStatsBetween(startDateTime, endDateTime);
        Map<LocalDate, Long> commentStatsMap = commentStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        // 일별 좋아요 통계 배치 조회
        List<Object[]> likeStats = postLikeRepository.getDailyLikeStatsBetween(startDate, endDate);
        Map<LocalDate, Long> likeStatsMap = likeStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        // 일별 조회수 통계 배치 조회
        List<Object[]> viewStats = postViewRepository.getDailyViewStatsBetween(startDate, endDate);
        Map<LocalDate, Long> viewStatsMap = viewStats.stream()
                .collect(Collectors.toMap(stat -> ((java.sql.Date) stat[0]).toLocalDate(),
                        stat -> ((Number) stat[1]).longValue()));

        // 모든 날짜에 대해 데이터 생성
        for (LocalDate date = startDateTime.toLocalDate(); !date.isAfter(endDateTime.toLocalDate());
                date = date.plusDays(1)) {
            Long totalPosts = totalPostsMap.getOrDefault(date, 0L);
            Long generalPosts = generalPostsMap.getOrDefault(date, 0L);
            Long pollPosts = pollPostsMap.getOrDefault(date, 0L);
            Long fairViewPosts = fairViewPostsMap.getOrDefault(date, 0L);
            Long comments = commentStatsMap.getOrDefault(date, 0L);
            Long likes = likeStatsMap.getOrDefault(date, 0L);
            Long views = viewStatsMap.getOrDefault(date, 0L);

            resultMap.put(date,
                    new DashboardDto.CommunityPostsData(date.toString(), totalPosts, generalPosts,
                            pollPosts, fairViewPosts, comments, likes, views));
        }

        return resultMap;
    }

    private DashboardDto.RecordingAIStatsDataDto getRecordingAIStatsDataBatch(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<DashboardDto.RecordingAIStatsData> recordingAIStatsData = new ArrayList<>();

        // 일별 녹음 통계 배치 조회
        List<RecordTrack> recordingStats =
                recordTrackRepository.findAllBetween(startDateTime, endDateTime);
        Map<LocalDate, DashboardDto.RecordingAIStatsData> resultMap = new HashMap<>();
        for (RecordTrack recordTrack : recordingStats) {
            LocalDate date = recordTrack.getCreatedAt()
                    .toLocalDate();
            DashboardDto.RecordingAIStatsData data = resultMap.getOrDefault(date, null);
            if (data == null) {
                data = new DashboardDto.RecordingAIStatsData(recordTrack);
                resultMap.put(date, data);
            }
            else {
                long updatedRecordings = data.getRecordings() + 1;
                long updatedAiReports = data.getAiReports() + (recordTrack.isAiGenerated() ? 1 : 0);
                data = new DashboardDto.RecordingAIStatsData(date.toString(), updatedRecordings,
                        updatedAiReports);
                resultMap.put(date, data);
            }
        }

        // 모든 날짜에 대해 데이터 생성 (빈 데이터도 포함)
        for (LocalDate date = startDateTime.toLocalDate(); !date.isAfter(endDateTime.toLocalDate());
                date = date.plusDays(1)) {
            DashboardDto.RecordingAIStatsData data = resultMap.getOrDefault(date,
                    new DashboardDto.RecordingAIStatsData(date.toString(), 0L, 0L));
            recordingAIStatsData.add(data);
        }

        return new DashboardDto.RecordingAIStatsDataDto(recordingAIStatsData);
    }

    private DashboardDto.UserGrowthData createUserGrowthDataForWeek(LocalDate weekStart,
            LocalDate weekEnd, String dateStr) {
        long realUsers =
                userRepository.findByTestUserIsFalseAndUnregisteredIsFalse(Pageable.unpaged())
                        .getTotalElements();
        long virtualUsers = userRepository.findByTestUserIsTrue(Pageable.unpaged())
                .getTotalElements();
        long totalUsers = realUsers + virtualUsers;

        long newRealUsers = userRepository.countBySignUpDateBetweenAndTestUserIsFalse(weekStart,
                weekEnd.plusDays(1));
        long newVirtualUsers = userRepository.countBySignUpDateBetweenAndTestUserIsTrue(weekStart,
                weekEnd.plusDays(1));
        long newTotalUsers = newRealUsers + newVirtualUsers;

        return new DashboardDto.UserGrowthData(dateStr, realUsers, virtualUsers, totalUsers,
                newRealUsers, newVirtualUsers, newTotalUsers);
    }

    private DashboardDto.UserGrowthData createUserGrowthDataForMonth(LocalDate monthStart,
            LocalDate monthEnd, String dateStr) {
        long realUsers =
                userRepository.findByTestUserIsFalseAndUnregisteredIsFalse(Pageable.unpaged())
                        .getTotalElements();
        long virtualUsers = userRepository.findByTestUserIsTrue(Pageable.unpaged())
                .getTotalElements();
        long totalUsers = realUsers + virtualUsers;

        long newRealUsers = userRepository.countBySignUpDateBetweenAndTestUserIsFalse(monthStart,
                monthEnd.plusDays(1));
        long newVirtualUsers = userRepository.countBySignUpDateBetweenAndTestUserIsTrue(monthStart,
                monthEnd.plusDays(1));
        long newTotalUsers = newRealUsers + newVirtualUsers;

        return new DashboardDto.UserGrowthData(dateStr, realUsers, virtualUsers, totalUsers,
                newRealUsers, newVirtualUsers, newTotalUsers);
    }

    private DashboardDto.CommunityPostsData createCommunityPostsDataForWeek(LocalDate weekStart,
            LocalDate weekEnd) {
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.plusDays(1)
                .atStartOfDay();

        long totalPosts = postRepository.countByCreatedAtBetween(weekStartTime, weekEndTime);
        long generalPosts =
                postRepository.countByCreatedAtBetweenAndPostType(weekStartTime, weekEndTime,
                        PostingType.GENERAL);
        long pollPosts =
                postRepository.countByCreatedAtBetweenAndPostType(weekStartTime, weekEndTime,
                        PostingType.POLL);
        long fairViewPosts =
                postRepository.countByCreatedAtBetweenAndPostType(weekStartTime, weekEndTime,
                        PostingType.FAIR_VIEW);

        long comments = commentRepository.countByCreatedAtBetween(weekStartTime, weekEndTime);
        long likes = postLikeRepository.countByCreatedAtBetween(weekStart, weekEnd.plusDays(1));
        long views = postViewRepository.countByCreatedAtBetween(weekStart, weekEnd.plusDays(1));

        return new DashboardDto.CommunityPostsData(weekStart.toString(), totalPosts, generalPosts,
                pollPosts, fairViewPosts, comments, likes, views);
    }

    private DashboardDto.CommunityPostsData createCommunityPostsDataForMonth(LocalDate monthStart,
            LocalDate monthEnd) {
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.plusDays(1)
                .atStartOfDay();

        long totalPosts = postRepository.countByCreatedAtBetween(monthStartTime, monthEndTime);
        long generalPosts =
                postRepository.countByCreatedAtBetweenAndPostType(monthStartTime, monthEndTime,
                        PostingType.GENERAL);
        long pollPosts =
                postRepository.countByCreatedAtBetweenAndPostType(monthStartTime, monthEndTime,
                        PostingType.POLL);
        long fairViewPosts =
                postRepository.countByCreatedAtBetweenAndPostType(monthStartTime, monthEndTime,
                        PostingType.FAIR_VIEW);

        long comments = commentRepository.countByCreatedAtBetween(monthStartTime, monthEndTime);
        long likes = postLikeRepository.countByCreatedAtBetween(monthStart, monthEnd.plusDays(1));
        long views = postViewRepository.countByCreatedAtBetween(monthStart, monthEnd.plusDays(1));

        return new DashboardDto.CommunityPostsData(monthStart.toString(), totalPosts, generalPosts,
                pollPosts, fairViewPosts, comments, likes, views);
    }

    private DashboardDto.RecordingAIStatsDataDto createRecordingAIStatsDataForWeek(
            LocalDate weekStart, LocalDate weekEnd) {
        List<DashboardDto.RecordingAIStatsData> recordingAIStatsData = new ArrayList<>();
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.plusDays(1)
                .atStartOfDay();

        List<RecordTrack> recordingStats =
                recordTrackRepository.findAllBetween(weekStartTime, weekEndTime);

        Map<LocalDate, DashboardDto.RecordingAIStatsData> resultMap = new HashMap<>();
        for (RecordTrack recordTrack : recordingStats) {
            //monthStartTime
            LocalDate weekMonday = recordTrack.getCreatedAt()
                    .toLocalDate()
                    .with(DayOfWeek.MONDAY);
            DashboardDto.RecordingAIStatsData data = resultMap.getOrDefault(weekMonday, null);
            if (data == null) {
                data = new DashboardDto.RecordingAIStatsData(recordTrack);
                resultMap.put(weekMonday, data);
            }
            else {
                long updatedRecordings = data.getRecordings() + 1;
                long updatedAiReports = data.getAiReports() + (recordTrack.isAiGenerated() ? 1 : 0);
                data = new DashboardDto.RecordingAIStatsData(weekMonday.toString(),
                        updatedRecordings, updatedAiReports);
                resultMap.put(weekMonday, data);
            }
        }

        // 모든 주에 대해 데이터 생성 (빈 데이터도 포함)
        for (int i = 7; i >= 0; i--) {
            LocalDate weekMonday = LocalDate.now()
                    .minusWeeks(i)
                    .with(DayOfWeek.MONDAY);
            DashboardDto.RecordingAIStatsData data = resultMap.getOrDefault(weekMonday,
                    new DashboardDto.RecordingAIStatsData(weekMonday.toString(), 0L, 0L));
            recordingAIStatsData.add(data);
        }

        return new DashboardDto.RecordingAIStatsDataDto(recordingAIStatsData);
    }

    private DashboardDto.RecordingAIStatsDataDto createRecordingAIStatsDataForMonth(
            LocalDate monthStart, LocalDate monthEnd) {
        List<DashboardDto.RecordingAIStatsData> recordingAIStatsData = new ArrayList<>();
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.plusDays(1)
                .atStartOfDay();

        List<RecordTrack> recordingStats =
                recordTrackRepository.findAllBetween(monthStartTime, monthEndTime);

        Map<LocalDate, DashboardDto.RecordingAIStatsData> resultMap = new HashMap<>();
        for (RecordTrack recordTrack : recordingStats) {
            //monthStartTime
            LocalDate monthFirstDate = recordTrack.getCreatedAt()
                    .toLocalDate()
                    .withDayOfMonth(1);
            DashboardDto.RecordingAIStatsData data = resultMap.getOrDefault(monthFirstDate, null);
            if (data == null) {
                data = new DashboardDto.RecordingAIStatsData(recordTrack);
                resultMap.put(monthFirstDate, data);
            }
            else {
                long updatedRecordings = data.getRecordings() + 1;
                long updatedAiReports = data.getAiReports() + (recordTrack.isAiGenerated() ? 1 : 0);
                data = new DashboardDto.RecordingAIStatsData(monthFirstDate.toString(),
                        updatedRecordings, updatedAiReports);
                resultMap.put(monthFirstDate, data);
            }
        }

        // 모든 월에 대해 데이터 생성 (빈 데이터도 포함)
        for (int i = 5; i >= 0; i--) {
            LocalDate monthFirstDate = LocalDate.now()
                    .minusMonths(i)
                    .withDayOfMonth(1);
            DashboardDto.RecordingAIStatsData data = resultMap.getOrDefault(monthFirstDate,
                    new DashboardDto.RecordingAIStatsData(monthFirstDate.toString(), 0L, 0L));
            recordingAIStatsData.add(data);
        }

        return new DashboardDto.RecordingAIStatsDataDto(recordingAIStatsData);
    }
}
