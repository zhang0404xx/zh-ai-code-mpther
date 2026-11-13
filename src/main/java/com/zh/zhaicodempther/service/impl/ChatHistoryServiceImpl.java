package com.zh.zhaicodempther.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zh.zhaicodempther.model.entity.ChatHistory;
import com.zh.zhaicodempther.mapper.ChatHistoryMapper;
import com.zh.zhaicodempther.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author zh
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
