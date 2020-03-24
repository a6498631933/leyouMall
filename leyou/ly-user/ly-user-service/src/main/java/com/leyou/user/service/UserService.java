package com.leyou.user.service;

import com.leyou.common.utils.CodeUtils;
import com.leyou.common.utils.GenerateCodeUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import org.omg.DynamicAny._DynEnumStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    static final String KEY_PREFIX = "user:code:phone:";

    static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public Boolean checkData(String data, Integer type) {
        User record = new User();
        switch (type) {
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                return null;
        }
        return this.userMapper.selectCount(record) == 0;
    }

    public Boolean sendVerifyCode(String phone) {
        // 生成验证码
        String code = GenerateCodeUtils.generateCode(6);
        try {
            // 发送短信
            Map<String, String> msg = new HashMap<>();
            msg.put("phone", phone);
            msg.put("code", code);
            this.amqpTemplate.convertAndSend("leyou.sms.exchange", "sms.verify.code", msg);
            // 将code存入redis
            this.redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.HOURS);
            return true;
        } catch (Exception e) {
            logger.error("发送短信失败。phone：{}， code：{}", phone, code);
            return false;
        }
    }

    public Boolean register(User user, String code) {
        String key = KEY_PREFIX + user.getPhone();

        //从 redis 提取出验证码
        String codeCache = this.redisTemplate.opsForValue().get(key);
        //检查验证码是否正确
        if(!code.equals(codeCache)) {
            //不正确
            return false;
        }

        user.setId(null);
        user.setCreated(new Date());

        //密码加密
        String encodePassword = CodeUtils.passwordBcryptEncode(user.getUsername().trim(), user.getPassword().trim());
        user.setPassword(encodePassword);

        //写入数据库
        boolean result = this.userMapper.insertSelective(user) == 1;
        if(result){
            try {
                this.redisTemplate.delete(KEY_PREFIX + user.getPhone());
            }catch (Exception e) {
                logger.error("删除缓存验证码失败，code:{}",code,e);
            }
        }
        return result;
    }

    public User queryUser(String username, String password) {
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);

        if(user == null){
            return null;
        }

        boolean result = CodeUtils.passwordConfirm(username + password,user.getPassword());
        if (!result){
            return null;
        }
        return user;
    }
}