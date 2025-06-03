package com.may21.trobl.notification.service;

import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMessageService {

    private final MessageSource messageSource;

    public NotificationDto.Message getMessage(NotificationType type, Locale locale, Map<String, Object> params) {
        return switch (type) {
            case COMMENT -> getCommentMessage(locale, params);
            case LIKE -> getLikeMessage(locale, params);
            case VOTE -> getVoteMessage(locale, params);
            case CONTENT_RECOMMENDATION -> getContentRecommendationMessage(locale, params);
            case POPULAR_POST -> getPopularPostMessage(locale, params);
            case FAIRVIEW_REQUEST -> getFairviewRequestMessage(locale, params);
            case QUICKPOLL_PARTICIPATION -> getQuickpollParticipationMessage(locale, params);
            case COMMUNITY_ANALYSIS -> getCommunityAnalysisMessage(locale, params);
            case ANNOUNCEMENT -> getAnnouncementMessage(locale, params);
            default -> new NotificationDto.Message(
                    messageSource.getMessage(type.getMessageKey() + ".title", null, locale),
                    messageSource.getMessage(type.getMessageKey() + ".body", null, locale));
        };
    }

    private NotificationDto.Message getVoteMessage(Locale locale, Map<String, Object> params) {
        if (params.containsKey("batchCount")) {
            // 일괄 처리 메시지
            int count = (Integer) params.get("batchCount");
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.vote.batch.title", null, locale),
                    messageSource.getMessage("notification.vote.batch.body", new Object[]{count}, locale));
        } else {
            // 개별 메시지 (즉시 전송용)
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.vote.title", null, locale),
                    messageSource.getMessage("notification.vote.body", null, locale));
        }
    }

    private NotificationDto.Message getCommentMessage(Locale locale, Map<String, Object> params) {
        if (params.containsKey("batchCount")) {
            // 일괄 처리 메시지
            int count = (Integer) params.get("batchCount");
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.comment.batch.title", null, locale),
                    messageSource.getMessage("notification.comment.batch.body", new Object[]{count}, locale));
        } else {
            // 개별 메시지 (즉시 전송용)
            String commenterName = (String) params.getOrDefault("commenterName", "익명");
            String contentSnippet = (String) params.getOrDefault("contentSnippet", "");
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.comment.title", null, locale),
                    messageSource.getMessage("notification.comment.body", new Object[]{commenterName, contentSnippet}, locale));
        }
    }

    private NotificationDto.Message getLikeMessage(Locale locale, Map<String, Object> params) {
        if (params.containsKey("batchCount")) {
            int count = (Integer) params.get("batchCount");
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.like.batch.title", null, locale),
                    messageSource.getMessage("notification.like.batch.body", new Object[]{count}, locale));
        } else {
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.like.title", null, locale),
                    messageSource.getMessage("notification.like.body", null, locale));
        }
    }

    private NotificationDto.Message getContentRecommendationMessage(Locale locale, Map<String, Object> params) {
        String postTitle = (String) params.getOrDefault("postTitle", "추천 게시글");
        return new NotificationDto.Message(
                messageSource.getMessage("notification.content_recommendation.title", new Object[]{postTitle}, locale),
                messageSource.getMessage("notification.content_recommendation.body", null, locale));
    }

    private NotificationDto.Message getPopularPostMessage(Locale locale, Map<String, Object> params) {
        return new NotificationDto.Message(
                messageSource.getMessage("notification.popular_post.title", null, locale),
                messageSource.getMessage("notification.popular_post.body", null, locale));
    }

    private NotificationDto.Message getFairviewRequestMessage(Locale locale, Map<String, Object> params) {
        // actionText가 있는 경우를 위해 3개 파라미터 생성자 사용 (actionText 필드가 있다고 가정)
        return new NotificationDto.Message(
                messageSource.getMessage("notification.fairview_request.body", null, locale),
                messageSource.getMessage("notification.fairview_request.action", null, locale));
    }

    private NotificationDto.Message getQuickpollParticipationMessage(Locale locale, Map<String, Object> params) {
        if (params.containsKey("batchCount")) {
            int count = (Integer) params.get("batchCount");
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.quickpoll_participation.batch.title", null, locale),
                    messageSource.getMessage("notification.quickpoll_participation.batch.body", new Object[]{count}, locale));
        } else {
            return new NotificationDto.Message(
                    messageSource.getMessage("notification.quickpoll_participation.title", null, locale),
                    messageSource.getMessage("notification.quickpoll_participation.body", null, locale));
        }
    }

    private NotificationDto.Message getCommunityAnalysisMessage(Locale locale, Map<String, Object> params) {
        return new NotificationDto.Message(
                messageSource.getMessage("notification.community_analysis.title", null, locale),
                messageSource.getMessage("notification.community_analysis.body", null, locale));
    }

    private NotificationDto.Message getAnnouncementMessage(Locale locale, Map<String, Object> params) {
        String title = (String) params.getOrDefault("announcementTitle", "공지사항");
        String body = (String) params.getOrDefault("announcementBody", "새로운 공지사항이 있습니다.");
        return new NotificationDto.Message(title, body);
    }
}

