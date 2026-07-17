package com.swift.controller;

import com.swift.common.Result;
import com.swift.entity.InboundMessage;
import com.swift.entity.OutboundMessage;
import com.swift.repository.InboundMessageMapper;
import com.swift.repository.OutboundMessageMapper;
import com.swift.util.XmlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final OutboundMessageMapper outboundRepo;
    private final InboundMessageMapper inboundRepo;

    @PostMapping("/outbound")
    public Result<Map<String, String>> outbound(@RequestBody Map<String, String> body) {
        String xml = body.get("xml");
        log.info("[发报接口] 收到发报请求，XML长度: {}", xml != null ? xml.length() : 0);
        
        if (xml == null || xml.trim().isEmpty()) {
            log.warn("[发报接口] 请求XML为空");
            return Result.businessError("xml 为空");
        }
        String msgId = XmlUtils.extractTag(xml, "BizMsgIdr");
        if (msgId.isEmpty()) {
            log.warn("[发报接口] 无法提取BizMsgIdr");
            return Result.businessError("无法提取 BizMsgIdr");
        }
        log.info("[发报接口] 解析报文成功，msgId: {}", msgId);
        
        try {
            OutboundMessage m = new OutboundMessage();
            m.setMsgId(msgId);
            m.setMsgType(XmlUtils.detectType(xml));
            m.setSenderBic(XmlUtils.extractBic(xml, "Fr"));
            m.setReceiverBic(XmlUtils.extractBic(xml, "To"));
            m.setRawContent(xml);
            m.setStatus(OutboundMessage.PENDING_ROUTING);
            
            outboundRepo.save(m);
            log.info("[发报接口] 报文已保存到outbound表，msgId: {}，type: {}", msgId, m.getMsgType());
            
            return Result.success(Map.of("msgId", msgId));
        } catch (DuplicateKeyException e) {
            log.warn("[发报接口] 报文已存在: {}", msgId);
            return Result.conflict("报文已存在: " + msgId);
        }
    }

    @GetMapping("/{msgId}")
    public Result<Map<String, Object>> get(@PathVariable String msgId) {
        log.info("[查询接口] 查询报文，msgId: {}", msgId);
        
        // 查询接口统一从 inbound 表获取
        InboundMessage m = inboundRepo.findByMsgId(msgId);
        if (m == null) {
            log.warn("[查询接口] 报文不存在: {}", msgId);
            return Result.notFound("报文不存在");
        }
        
        log.info("[查询接口] 查询成功，msgId: {}，type: {}，status: {}", 
                 m.getMsgId(), m.getMsgType(), m.getStatus());
        
        // 只返回 msgId, msgType, rawContent 三个字段
        Map<String, Object> responseData = Map.of(
            "msgId", m.getMsgId(),
            "msgType", m.getMsgType(),
            "rawContent", m.getRawContent()
        );
        
        return Result.success(responseData);
    }
}