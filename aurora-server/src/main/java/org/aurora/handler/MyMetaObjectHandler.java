package org.aurora.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.aurora.constant.StatusConstant;
import org.aurora.context.BaseContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = null;
        try {
            userId = BaseContext.getCurrentId();
        } catch (Exception e) {
            e.printStackTrace();
            userId = -1L;//表示是自己创建
        }
        this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("createUser", userId, metaObject);
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateUser", userId, metaObject);
        this.setFieldValByName("status", StatusConstant.ENABLE, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateUser", BaseContext.getCurrentId(), metaObject);
    }
}