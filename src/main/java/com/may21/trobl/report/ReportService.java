package com.may21.trobl.report;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.post.domain.Posting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;

    public int report(Long userId, Long targetId, ItemType targetType,
            ReportDto.Request reportRequest) {
        if (!reportRepository.existsByTargetIdAndTargetTypeAndReportedBy(targetId, targetType, userId)) {

            Report report = new Report(targetId, targetType, userId, reportRequest);
            reportRepository.save(report);
        }
        return reportRepository.countReportByTargetIdAndTargetType(targetId, targetType) + 1;
    }

    public List<Long> getBlockedTargetIds(Long userId, List<Long> targetIds, ItemType targetType) {
        return reportRepository.findBlockedIdsByUserIdAndTargetTypeInTargetIds(userId, targetType, targetIds);
    }

    public List<Long> getBlockedTargetIds(Long userId, ItemType targetType) {
        return reportRepository.findIdsByReportedByAndTargetType(userId, targetType);
    }

    public List<Comment> filterBlockedComments(Long userId, List<Comment> comments) {
        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .toList();
        List<Report> reports = reportRepository.getRelatedReports(userId, commentIds, ItemType.COMMENT, ItemType.USER);
        List<Long> blockedCommentIds = reports.stream()
                .filter(report -> report.getTargetType() == ItemType.COMMENT)
                .map(Report::getTargetId)
                .toList();
        List<Long> blockedUserIds = reports.stream()
                .filter(report -> report.getTargetType() == ItemType.USER)
                .map(Report::getTargetId)
                .toList();
        if (!blockedCommentIds.isEmpty()) {
            comments = comments.stream()
                    .filter(comment -> !blockedCommentIds.contains(comment.getId())
                            && !blockedUserIds.contains(comment.getUserId()))
                    .toList();
        }
        return comments;
    }

    public List<Posting> filterBlockedPosts(Long userId, List<Posting> posts) {
        List<Long> postIds = posts.stream()
                .map(Posting::getId)
                .toList();
        List<Report> reports = reportRepository.getRelatedReports(userId, postIds, ItemType.POST, ItemType.USER);
        List<Long> blockedPostIds = reports.stream()
                .filter(report -> report.getTargetType() == ItemType.POST)
                .map(Report::getTargetId)
                .toList();
        List<Long> blockedUserIds = reports.stream()
                .filter(report -> report.getTargetType() == ItemType.USER)
                .map(Report::getTargetId)
                .toList();
        if (!blockedPostIds.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> !blockedPostIds.contains(post.getId())
                            && !blockedUserIds.contains(post.getUserId()))
                    .toList();
        }
        return posts;
    }

}
