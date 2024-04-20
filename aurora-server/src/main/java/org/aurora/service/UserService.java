package org.aurora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.UserLoginDTO;
import org.aurora.entity.User;

/**
 * 用户信息(User)表服务接口
 *
 * @author Aurora
 * @since 2024-04-17 15:26:15
 */
public interface UserService extends IService<User> {

    User wxLogin(UserLoginDTO userLoginDTO);
}

