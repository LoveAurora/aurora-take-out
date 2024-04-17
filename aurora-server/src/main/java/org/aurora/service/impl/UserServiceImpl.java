package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.User;
import org.aurora.mapper.UserMapper;
import org.aurora.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户信息(User)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:15
 */
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}

