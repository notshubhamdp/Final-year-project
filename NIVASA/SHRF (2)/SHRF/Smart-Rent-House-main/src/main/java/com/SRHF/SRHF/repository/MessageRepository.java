package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages in a conversation between two users for a specific property
     * Returns messages where sender is user1 and receiver is user2, OR sender is user2 and receiver is user1
     */
    @Query("SELECT m FROM Message m WHERE m.propertyId = :propertyId AND " +
           "((m.senderId = :user1 AND m.receiverId = :user2) OR " +
           "(m.senderId = :user2 AND m.receiverId = :user1)) " +
           "ORDER BY m.timestamp ASC")
    List<Message> findConversationMessages(
            @Param("propertyId") Long propertyId,
            @Param("user1") Long user1,
            @Param("user2") Long user2);

    List<Message> findBySenderIdOrReceiverIdOrderByTimestampDesc(Long senderId, Long receiverId);

    Long countByReceiverIdAndIsReadFalse(Long receiverId);

    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    void deleteBySenderIdOrReceiverId(Long senderId, Long receiverId);
}
