package cn.egret.service.Impl;

import cn.egret.domain.User;
import cn.egret.annotation.Service;
import cn.egret.annotation.Transactional;
import cn.egret.helper.DatabaseHelper;
import cn.egret.service.IUserService;

import java.util.List;
import java.util.Map;

/**
 * @author egret
 */
@Service
public class UserService implements IUserService {
    /**
     * 获取所有用户
     */
    @Override
    public List<User> getAllUser() {
        String sql = "SELECT * FROM user";
        return DatabaseHelper.queryEntityList(User.class, sql);
    }

    /**
     * 根据id获取用户信息
     */
    @Override
    public User GetUserInfoById(Integer id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        Class stringClass = String.class;
        return DatabaseHelper.queryEntity(User.class, sql, id);
    }

    /**
     * 修改用户信息
     */
    @Override
    @Transactional
    public boolean updateUser(int id, Map<String, Object> fieldMap) {
        int a = 1/0;
        return DatabaseHelper.updateEntity(User.class, id, fieldMap);
    }
}
