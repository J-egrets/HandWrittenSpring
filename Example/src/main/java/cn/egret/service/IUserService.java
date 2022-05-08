package cn.egret.service;

import cn.egret.domain.User;

import java.util.List;
import java.util.Map;

/**
 * @author egret
 */
public interface IUserService {
    List<User> getAllUser();

    User GetUserInfoById(Integer id);

    boolean updateUser(int id, Map<String, Object> fieldMap);
}
