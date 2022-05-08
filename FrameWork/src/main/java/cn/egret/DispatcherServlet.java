package cn.egret;


import com.alibaba.fastjson.JSON;
import cn.egret.bean.Data;
import cn.egret.bean.Handler;
import cn.egret.bean.Param;
import cn.egret.bean.View;
import cn.egret.helper.BeanHelper;
import cn.egret.helper.ConfigHelper;
import cn.egret.helper.ControllerHelper;
import cn.egret.helper.RequestHelper;
import cn.egret.util.ReflectionUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 请求转发器
 * 该Servlet将会在Web容器启动时加载
 * 前端控制器实际上是一个Servlet, 这里配置的是拦截所有请求, 在服务器启动时实例化.
 *
 * 当DispatcherServlet实例化时, 首先执行 init() 方法,
 * 这时会调用 HelperLoader.init() 方法来加载相关的helper类，并注册处理相应资源的Servlet.
 *
 * 对于每一次客户端请求都会执行 service() 方法, 这时会首先将请求方法和请求路径封装为Request对象,
 * 然后从映射处理器 (REQUEST_MAP) 中获取到处理器.
 * 然后从客户端请求中获取到Param参数对象, 执行处理器方法. 最后判断处理器方法的返回值,
 * 若为view类型, 则跳转到jsp页面, 若为data类型, 则返回json数据.
 *
 * 相当于在web.xml文件里面配置了servlet
 * 在Servlet中,设置了@WebServlet注解,当请求该Servlet时,服务器就会自动读取当中的信息,
 * 如果注解@WebServlet("/category"),则表示该Servlet默认的请求路径为…/category,
 * 这里省略了urlPatterns属性名,完整的写法应该是:@WebServlet(urlPatterns = “/category”),
 * 如果在@WebServlet中需要设置多个属性,必须给属性值加上属性名称,中间用逗号隔开,否则会报错.
 * 若没有设置@WebServlet的name属性，默认值会是Servlet的类完整名称.
 *
 * name		指定Servlet 的 name 属性，等价于 <servlet-name>。如果没有显式指定，则该 Servlet 的取值即为类的全限定名。
 * value	该属性等价于 urlPatterns 属性。两个属性不能同时使用。
 * urlPatterns	指定一组 Servlet 的 URL 匹配模式。等价于<url-pattern>标签。
 * loadOnStartup	指定 Servlet 的加载顺序，等价于 <load-on-startup>标签。
 * initParams	指定一组 Servlet 初始化参数，等价于<init-param>标签。
 * asyncSupported	声明 Servlet 是否支持异步操作模式，等价于<async-supported> 标签。
 * description	该 Servlet 的描述信息，等价于 <description>标签。
 * displayName	该 Servlet 的显示名，通常配合工具使用，等价于 <display-name>标签。
 * @author egret
 */
@WebServlet(urlPatterns = "/*", loadOnStartup = 0)
public class DispatcherServlet extends HttpServlet {

    @Override
    public void init(ServletConfig servletConfig) {
        //初始化相关的helper类
        HelperLoader.init();

        //获取ServletContext对象, 用于注册Servlet
        ServletContext servletContext = servletConfig.getServletContext();

        //注册处理jsp和静态资源的servlet
        registerServlet(servletContext);
    }

    /**
     * DefaultServlet和JspServlet都是由Web容器创建
     * org.apache.catalina.servlets.DefaultServlet
     * org.apache.jasper.servlet.JspServlet
     */
    private void registerServlet(ServletContext servletContext) {
        //动态注册处理JSP的Servlet
        ServletRegistration jspServlet = servletContext.getServletRegistration("jsp");
        jspServlet.addMapping(ConfigHelper.getAppJspPath() + "*");

        //动态注册处理静态资源的默认Servlet
        ServletRegistration defaultServlet = servletContext.getServletRegistration("default");
        //网站头像
        defaultServlet.addMapping("/favicon.ico");
        defaultServlet.addMapping(ConfigHelper.getAppAssetPath() + "*");
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 获取请求方式
        String requestMethod = request.getMethod().toUpperCase();

        // 这个方法返回请求的实际URL相对于请求的serlvet的url的路径。
        // 为servlet配置的访问路径是：/servlet/test/*
        // 我用这个 URL 来访问它：http://localhost:8080/dwr/servlet/test/joejoe1991/a.html
        // 这个实际的URL，相对于那个servlet 的url （"/servlet/test/*"）的路径是：joejoe1991/a.html
        // 所以 request.getPathInfo() 方法返回的就是："/joejoe1991/a.html"
        // 如果你的URL里有查询字符串，getPathInfo() 方法并不返回这些查询字符串
        // http://localhost:8080/dwr/servlet/test/joejoe1991/a.html?name=test
        // getPathInfo() 返回的仍然是："/joejoe1991/a.html" ，而并不包括后面的"?name=test"
        String requestPath = request.getPathInfo();

        //这里根据Tomcat的配置路径有两种情况, 一种是 "/userList", 另一种是 "/context地址/userList".
        String[] splits = requestPath.split("/");
        if (splits.length > 2) {
            requestPath = "/" + splits[2];
        }

        //根据请求获取处理器(这里类似于SpringMVC中的映射处理器)
        Handler handler = ControllerHelper.getHandler(requestMethod, requestPath);
        if (handler != null) {
            // 获取controller类
            Class<?> controllerClass = handler.getControllerClass();
            // 从spring容器中获取been
            Object controllerBean = BeanHelper.getBean(controllerClass);

            //初始化参数
            Param param = RequestHelper.createParam(request);

            //调用与请求对应的方法(这里类似于SpringMVC中的处理器适配器)
            Object result;
            // 获取控制器方法
            Method actionMethod = handler.getControllerMethod();
            if (param == null || param.isEmpty()) {
                // http请求没有携带参数
                result = ReflectionUtil.invokeMethod(controllerBean, actionMethod);
            } else {
                // http请求有携带参数
                // Controller方法的参数都封装到Param类中了
                result = ReflectionUtil.invokeMethod(controllerBean, actionMethod, param);
            }

            //跳转页面或返回json数据(这里类似于SpringMVC中的视图解析器)
            if (result instanceof View) {
                handleViewResult((View) result, request, response);
            } else if (result instanceof Data) {
                handleDataResult((Data) result, response);
            }
        }
    }

    /**
     * 跳转页面
     */
    private void handleViewResult(View view, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 跳转路径
        String path = view.getPath();
        if (StringUtils.isNotEmpty(path)) {
            if (path.startsWith("/")) {
                // 重定向
                response.sendRedirect(request.getContextPath() + path);
            } else { //请求转发
                Map<String, Object> model = view.getModel();
                for (Map.Entry<String, Object> entry : model.entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
                request.getRequestDispatcher(ConfigHelper.getAppJspPath() + path).forward(request, response);
            }
        }
    }

    /**
     * 返回JSON数据
     */
    private void handleDataResult(Data data, HttpServletResponse response) throws IOException {
        Object model = data.getModel();
        if (model != null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            String json = JSON.toJSON(model).toString();
            writer.write(json);
            writer.flush();
            writer.close();
        }
    }
}
