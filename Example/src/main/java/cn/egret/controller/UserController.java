package cn.egret.controller;

import cn.egret.domain.User;
import cn.egret.annotation.Autowired;
import cn.egret.annotation.Controller;
import cn.egret.annotation.RequestMapping;
import cn.egret.annotation.RequestMethod;
import cn.egret.bean.Data;
import cn.egret.bean.Param;
import cn.egret.bean.View;
import cn.egret.service.IUserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author egret
 */
@Controller
public class UserController {

    @Autowired
    private IUserService userService;

    public IUserService getUserService() {
        return userService;
    }

    /**
     * 用户列表
     *
     * @return
     */
    @RequestMapping(value = "/userList", method = RequestMethod.GET)
    public View getUserList() {
        List<User> userList = userService.getAllUser();
        return new View("index.jsp").addModel("userList", userList);
    }

    /**
     * 用户详情
     *
     * @param param
     * @return
     */
    @RequestMapping(value = "/userInfo", method = RequestMethod.GET)
    public Data getUserInfo(Param param) {
        String id = (String) param.getParamMap().get("id");
        User user = userService.GetUserInfoById(Integer.parseInt(id));

        return new Data(user);
    }

    @RequestMapping(value = "/userEdit", method = RequestMethod.GET)
    public Data editUser(Param param) {
        String id = (String) param.getParamMap().get("id");
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("age", 911);
        userService.updateUser(Integer.parseInt(id), fieldMap);

        return new Data("Success.");
    }

}
