package org.aurora.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.constant.MessageConstant;
import org.aurora.dto.UserLoginDTO;
import org.aurora.entity.User;
import org.aurora.exception.LoginFailedException;
import org.aurora.mapper.UserMapper;
import org.aurora.properties.WeChatProperties;
import org.aurora.service.UserService;
import org.aurora.utils.HttpClientUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息(User)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:15
 */
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    private final WeChatProperties weChatProperties;
    private final UserMapper userMapper;

    public UserServiceImpl(WeChatProperties weChatProperties, UserMapper userMapper) {
        this.weChatProperties = weChatProperties;
        this.userMapper = userMapper;
    }


    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openid是否为空，如果为空表示登录失败，抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenid, openid);
        //判断当前用户是否为新用户
        User user = getOne(queryWrapper);
        //如果是新用户，自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回这个用户对象
        return user;
    }

    /**
     * 调用微信接口服务，获取微信用户的openid
     */
    private String getOpenid(String code) {
        //调用微信接口服务，获得当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);

        return jsonObject.getString("openid");
    }

}