package com.swift.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swift.entity.InboundMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 收到报文的数据访问层
 * 操作 inbound_message 表
 */
@Mapper
public interface InboundMessageMapper extends BaseMapper<InboundMessage> {

    /** 插入收到的报文 */
    default void save(InboundMessage m) {
        this.insert(m);
    }

    /** 按 msgId 查询单条收到的报文 */
    default InboundMessage findByMsgId(String msgId) {
        return selectOne(new LambdaQueryWrapper<InboundMessage>().eq(InboundMessage::getMsgId, msgId));
    }

    /** 按状态查询收到的报文，按 id 升序并限制条数 */
    default List<InboundMessage> findByStatus(String status, int limit) {
        return selectList(new LambdaQueryWrapper<InboundMessage>()
                .eq(InboundMessage::getStatus, status)
                .orderByAsc(InboundMessage::getId)
                .last("LIMIT " + limit));
    }

    /** 更新收到报文的状态 */
    default void updateStatus(Long id, String status) {
        update(null, new LambdaUpdateWrapper<InboundMessage>()
                .set(InboundMessage::getStatus, status)
                .eq(InboundMessage::getId, id));
    }

    /** 回退到待处理并递增重试次数 */
    default void retry(Long id) {
        update(null, new LambdaUpdateWrapper<InboundMessage>()
                .set(InboundMessage::getStatus, InboundMessage.PENDING_ROUTING)
                .setSql("retry_count = retry_count + 1")
                .eq(InboundMessage::getId, id));
    }

    /** 仅递增重试次数，保持当前状态不变（用于来报异步重试） */
    default void incrementRetryKeepStatus(Long id) {
        update(null, new LambdaUpdateWrapper<InboundMessage>()
                .setSql("retry_count = retry_count + 1")
                .eq(InboundMessage::getId, id));
    }
}