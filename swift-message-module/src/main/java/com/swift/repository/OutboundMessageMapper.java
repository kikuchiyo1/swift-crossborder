package com.swift.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swift.entity.OutboundMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 待发送报文的数据访问层
 * 操作 outbound_message 表
 */
@Mapper
public interface OutboundMessageMapper extends BaseMapper<OutboundMessage> {

    /** 插入待发送的报文 */
    default void save(OutboundMessage m) {
        this.insert(m);
    }

    /** 按 msgId 查询单条待发送的报文 */
    default OutboundMessage findByMsgId(String msgId) {
        return selectOne(new LambdaQueryWrapper<OutboundMessage>().eq(OutboundMessage::getMsgId, msgId));
    }

    /** 按状态查询待发送的报文，按 id 升序并限制条数 */
    default List<OutboundMessage> findByStatus(String status, int limit) {
        return selectList(new LambdaQueryWrapper<OutboundMessage>()
                .eq(OutboundMessage::getStatus, status)
                .orderByAsc(OutboundMessage::getId)
                .last("LIMIT " + limit));
    }

    /** 更新待发送报文的状态 */
    default void updateStatus(Long id, String status) {
        update(null, new LambdaUpdateWrapper<OutboundMessage>()
                .set(OutboundMessage::getStatus, status)
                .eq(OutboundMessage::getId, id));
    }

    /** 回退到待处理并递增重试次数 */
    default void retry(Long id) {
        update(null, new LambdaUpdateWrapper<OutboundMessage>()
                .set(OutboundMessage::getStatus, OutboundMessage.PENDING_ROUTING)
                .setSql("retry_count = retry_count + 1")
                .eq(OutboundMessage::getId, id));
    }
}