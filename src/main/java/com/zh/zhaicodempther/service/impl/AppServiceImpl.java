package com.zh.zhaicodempther.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zh.zhaicodempther.model.entity.App;
import com.zh.zhaicodempther.mapper.AppMapper;
import com.zh.zhaicodempther.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author zh
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
