package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.properties.JwtProperties;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    @Qualifier("JwtPropertiesLeyou")
    private JwtProperties properties;

    public String authentication(String username, String password) {
        try {
            //调用微服务， 执行查询
            User user = this.userClient.queryUser(username, password);

            if(user == null){
                return null;
            }

            String token = JwtUtils.generateToken(new UserInfo(user.getId(),user.getUsername()), properties.getPrivateKey(), properties.getExpire());
            return token;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
