package com.may21.trobl.notification.domain;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.UpdateType;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentUpdateService {
    private final ContentUpdateRepository contentUpdateRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;


    public void readIfExist(Long userId, Long targetId, ItemType itemType) {
        contentUpdateRepository.deleteAllByUserIdAndTargetIdAndTargetType(userId, targetId,
                itemType);
    }

    public void readIfExist(Long userId, List<Long> commentIds, ItemType itemType) {
        if (commentIds.isEmpty()) {
            return;
        }
        for (Long commentId : commentIds) {
            contentUpdateRepository.deleteAllByUserIdAndTargetIdAndTargetType(userId, commentId,
                    itemType);
        }
    }

    @Transactional
    public void commentUpdate(Long postId, CommentDto.Request request) {
        List<ContentUpdate> contentUpdates = new ArrayList<>();
        Long commentId = request.getCommentId();
        if (commentId != null) {
            Long commentUserId = commentRepository.getOwnerIdByCommentId(commentId);
            if (!contentUpdateRepository.existByUserIdAndTargetIdAndTargetType(commentUserId,
                    commentId, ItemType.COMMENT

            ) && commentUserId != null && userRepository.findById(commentUserId)
                    .isPresent()) {
                ContentUpdate contentUpdate =
                        new ContentUpdate(commentUserId, commentId, ItemType.COMMENT,
                                UpdateType.COMMENT);
                contentUpdates.add(contentUpdate);
            }
        }
        Long postOwnerId = postRepository.getPostOwnerIdByPostId(postId);
        if (!contentUpdateRepository.existByUserIdAndTargetIdAndTargetType(postOwnerId, postId,
                ItemType.POST

        ) && postOwnerId != null && userRepository.findById(postOwnerId)
                .isPresent()) {
            ContentUpdate contentUpdate =
                    new ContentUpdate(postOwnerId, postId, ItemType.POST, UpdateType.COMMENT);
            contentUpdates.add(contentUpdate);
        }
        if (!contentUpdates.isEmpty()) {
            contentUpdateRepository.saveAll(contentUpdates);
        }
    }

    @Transactional
    public void likeUpdate(Long targetId, ItemType itemType) {
        Long targetUserId = switch (itemType) {
            case POST -> postRepository.getPostOwnerIdByPostId(targetId);
            case COMMENT -> commentRepository.getOwnerIdByCommentId(targetId);
            default -> null;
        };
        if (targetUserId == null) {
            return;
        }
        if (!contentUpdateRepository.existByUserIdAndTargetIdAndTargetType(targetUserId, targetId,
                itemType) && userRepository.findById(targetUserId)
                .isPresent()) {
            ContentUpdate contentUpdate =
                    new ContentUpdate(targetUserId, targetId, itemType, UpdateType.LIKE);
            contentUpdateRepository.save(contentUpdate);

        }

    }

    @Transactional
    public void fairViewConfirmUpdate(Long postId, Long targetUserId) {
        if (userRepository.findById(targetUserId)
                .isPresent() &&
                !contentUpdateRepository.existByUserIdAndTargetIdAndTargetType(targetUserId, postId,
                        ItemType.POST)) {
            ContentUpdate contentUpdate =
                    new ContentUpdate(targetUserId, postId, ItemType.POST, UpdateType.CONFIRMED);
            contentUpdateRepository.save(contentUpdate);
        }
    }

    @Transactional
    public void fairViewRequestUpdate(Long postId, Long targetUserId) {
        if (userRepository.findById(targetUserId)
                .isPresent() &&
                !contentUpdateRepository.existByUserIdAndTargetIdAndTargetType(targetUserId, postId,
                        ItemType.POST)) {
            ContentUpdate contentUpdate =
                    new ContentUpdate(targetUserId, postId, ItemType.POST, UpdateType.REQUESTED);
            contentUpdateRepository.save(contentUpdate);
        }
    }

    public boolean hasUserNewUpdates(Long userId) {
        return contentUpdateRepository.existsByUserId(userId);
    }

    public NotificationDto.SubMenu getSubManuNotification(Long userId) {
        List<ContentUpdate> contentUpdates = contentUpdateRepository.findByUserId(userId);
        boolean myPost = false;
        boolean myComment = false;
        boolean requestedPost = false;
        for (ContentUpdate contentUpdate : contentUpdates) {
            UpdateType changeType = contentUpdate.getChangeType();
            if (contentUpdate.getTargetType()
                    .equals(ItemType.POST)) {
                if (!requestedPost && (changeType == UpdateType.REQUESTED ||
                        changeType == UpdateType.CONFIRMED)) {
                    requestedPost = true;
                }
                else if (!myPost &&
                        (changeType == UpdateType.COMMENT || changeType == UpdateType.LIKE)) {
                    myPost = true;
                }

            }
            else if (!myComment && contentUpdate.getTargetType()
                    .equals(ItemType.COMMENT)) myComment = true;
        }

        return new NotificationDto.SubMenu(myPost, myComment, requestedPost);
    }

    public <T> Map<Long, NotificationDto.ContentUpdateStatus> getContentUpdatesByUserId(Long userId,
            List<T> itemList, Function<T, Long> idExtractor, ItemType itemType) {
        List<Long> itemIds = itemList.stream()
                .map(idExtractor)
                .toList();
        List<ContentUpdate> contentUpdates =
                contentUpdateRepository.findByUserIdInTargetIdsAndTargetType(userId, itemIds,
                        itemType);

        Map<Long, NotificationDto.ContentUpdateStatus> contentUpdateStatuses = new HashMap<>();

        for (ContentUpdate contentUpdate : contentUpdates) {
            contentUpdateStatuses.computeIfAbsent(contentUpdate.getTargetId(),
                            id -> new NotificationDto.ContentUpdateStatus(contentUpdate))
                    .update(contentUpdate);
        }

        for (T item : itemList) {
            Long id = idExtractor.apply(item);
            contentUpdateStatuses.computeIfAbsent(id, NotificationDto.ContentUpdateStatus::new);
        }

        return contentUpdateStatuses;
    }

    @Transactional
    public void deleteItem(Long itemId, ItemType itemType) {
        contentUpdateRepository.deleteAllByTargetIdAndTargetType(itemId, itemType);
    }
}
