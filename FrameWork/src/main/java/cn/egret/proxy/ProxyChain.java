package cn.egret.proxy;

import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是一个代理链类, proxyList 存储的是代理列表(也就是增强列表), 当执行doProxyChain() 方法时会按照顺序执行增强, 最后再执行目标方法.
 * @author egret
 */
public class ProxyChain {

    /**
     * 目标类
     */
    private final Class<?> targetClass;

    /**
     * 目标对象
     */
    private final Object targetObject;

    /**
     * 目标方法
     */
    private final Method targetMethod;

    /**
     * 方法代理
     */
    private final MethodProxy methodProxy;

    /**
     * 方法参数
     */
    private final Object[] methodParams;

    /**
     * 代理列表
     */
    private List<Proxy> proxyList = new ArrayList<>();

    /**
     * 代理索引
     */
    private int proxyIndex = 0;

    public ProxyChain(Class<?> targetClass, Object targetObject, Method targetMethod, MethodProxy methodProxy, Object[] methodParams, List<Proxy> proxyList) {
        this.targetClass = targetClass;
        this.targetObject = targetObject;
        this.targetMethod = targetMethod;
        this.methodProxy = methodProxy;
        this.methodParams = methodParams;
        this.proxyList = proxyList;
    }

    public Object[] getMethodParams() {
        return methodParams;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    /**
     * 递归执行
     * CGLIB的核心类：
     *     net.sf.cglib.proxy.Enhancer – 主要的增强类
     *     net.sf.cglib.proxy.MethodInterceptor – 主要的方法拦截类，它是Callback接口的子接口，需要用户实现
     *     net.sf.cglib.proxy.MethodProxy – JDK的java.lang.reflect.Method类的代理类，可以方便的实现对源对象方法的调用,如使用：
     *     Object o = methodProxy.invokeSuper(proxy, args);//虽然第一个参数是被代理对象，也不会出现死循环的问题。
     */
    public Object doProxyChain() throws Throwable {
        Object methodResult;
        if (proxyIndex < proxyList.size()) {
            //执行增强方法
            methodResult = proxyList.get(proxyIndex++).doProxy(this);
        } else {
            //目标方法最后执行且只执行一次
            methodResult = methodProxy.invokeSuper(targetObject, methodParams);
        }
        return methodResult;
    }
}