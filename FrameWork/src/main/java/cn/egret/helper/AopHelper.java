package cn.egret.helper;

import cn.egret.annotation.Aspect;
import cn.egret.annotation.Service;
import cn.egret.proxy.AspectProxy;
import cn.egret.proxy.Proxy;
import cn.egret.proxy.ProxyFactory;
import cn.egret.proxy.TransactionProxy;
import cn.egret.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 切面编程助手类
 * AopHelper 助手类用来初始化整个AOP框架, 逻辑如下:
 *
 * 框架中所有Bean的实例都是从Bean容器中获取, 然后再执行该实例的方法, 基于此,
 * 初始化AOP框架实际上就是用代理对象覆盖掉Bean容器中的目标对象, 这样根据目标类的Class对象从Bean容器中获取到的就是代理对象, 从而达到了对目标对象增强的目的.
 * @author egret
 */
public final class AopHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AopHelper.class);

    static {
        try {
            //切面类-目标类集合的映射
            Map<Class<?>, Set<Class<?>>> aspectMap = createAspectMap();
            //目标类-切面对象列表的映射
            Map<Class<?>, List<Proxy>> targetMap = createTargetMap(aspectMap);

            //把切面对象织入到目标类中, 创建代理对象
            for (Map.Entry<Class<?>, List<Proxy>> targetEntry : targetMap.entrySet()) {
                // 获取目标类
                Class<?> targetClass = targetEntry.getKey();
                // 获取切面对象列表
                List<Proxy> proxyList = targetEntry.getValue();

                // 创建代理类
                Object proxy = ProxyFactory.createProxy(targetClass, proxyList);

                //覆盖Bean容器里目标类对应的实例, 下次从Bean容器获取的就是代理对象了
                BeanHelper.setBean(targetClass, proxy);
            }
        } catch (Exception e) {
            LOGGER.error("aop failure", e);
        }
    }

    /**
     * 获取切面类-目标类集合的映射
     */
    private static Map<Class<?>, Set<Class<?>>> createAspectMap() throws Exception {
        Map<Class<?>, Set<Class<?>>> aspectMap = new HashMap<>();
        // 获取普通切面类-目标类集合的映射
        addAspectProxy(aspectMap);
        // 获取事务切面类-目标类集合的映射
        addTransactionProxy(aspectMap);
        return aspectMap;
    }

    /**
     *  获取普通切面类-目标类集合的映射
     */
    private static void addAspectProxy(Map<Class<?>, Set<Class<?>>> aspectMap) throws Exception {
        //所有实现了AspectProxy抽象类的切面
        Set<Class<?>> aspectClassSet = ClassHelper.getClassSetBySuper(AspectProxy.class);

        for (Class<?> aspectClass : aspectClassSet) {
            // 实现了AspectProxy抽象类并且上面有@Aspect注解
            if (aspectClass.isAnnotationPresent(Aspect.class)) {
                // 获取这个类上面的注解信息
                Aspect aspect = aspectClass.getAnnotation(Aspect.class);
                //与该切面对应的目标类集合，根据@Aspect定义的包名和类名去获取对应的目标类集合
                Set<Class<?>> targetClassSet = createTargetClassSet(aspect);
                // 将切面类-目标类（切面要增强的某包的某类）添加到容器中
                aspectMap.put(aspectClass, targetClassSet);
            }
        }
    }

    /**
     *  获取事务切面类-目标类集合的映射
     *  事务代理相比普通代理的差别是, 我们默认所有Service对象都被代理了, 也就是说通过Service的Class对象,
     *  从Bean容器中得到的都是代理对象, 我们在执行代理方法时会判断目标方法上是否存在 @Transactional 注解,
     *  有就加上事务管理, 没有就直接执行.
     */
    private static void addTransactionProxy(Map<Class<?>, Set<Class<?>>> aspectMap) {
        Set<Class<?>> serviceClassSet = ClassHelper.getClassSetByAnnotation(Service.class);
        aspectMap.put(TransactionProxy.class, serviceClassSet);
    }

    /**
     * 根据@Aspect定义的包名和类名去获取对应的目标类集合
     */
    private static Set<Class<?>> createTargetClassSet(Aspect aspect) throws Exception {
        Set<Class<?>> targetClassSet = new HashSet<>();
        // 包名
        String pkg = aspect.pkg();
        // 类名
        String cls = aspect.cls();
        // 如果包名与类名均不为空，则添加指定类
        if (!pkg.equals("") && !cls.equals("")) {
            targetClassSet.add(Class.forName(pkg + "." + cls));
        } else if (!pkg.equals("")) {
            // 如果包名不为空, 类名为空, 则添加该包名下所有类
            targetClassSet.addAll(ClassUtil.getClassSet(pkg));
        }
        return targetClassSet;
    }

    /**
     * 将切面类-目标类集合的映射关系 转化为 目标类-切面对象列表的映射关系
     */
    private static Map<Class<?>, List<Proxy>> createTargetMap(Map<Class<?>, Set<Class<?>>> aspectMap) throws Exception {
        Map<Class<?>, List<Proxy>> targetMap = new HashMap<>();

        for (Map.Entry<Class<?>, Set<Class<?>>> proxyEntry : aspectMap.entrySet()) {
            //切面类
            Class<?> aspectClass = proxyEntry.getKey();
            //目标类集合
            Set<Class<?>> targetClassSet = proxyEntry.getValue();
            //创建目标类-切面对象列表的映射关系
            for (Class<?> targetClass : targetClassSet) {
                //切面对象，class对象的无参构造创建新实例
                Proxy aspect = (Proxy) aspectClass.newInstance();

                if (targetMap.containsKey(targetClass)) {
                    targetMap.get(targetClass).add(aspect);
                } else {
                    //切面对象列表
                    List<Proxy> aspectList = new ArrayList<>();
                    // 这里添加的aspect其实可以看做是完成了的切面类，继承关系如下：
                    // Proxy->AspectProxy->切面类（继承了AspectProxy）
                    aspectList.add(aspect);
                    targetMap.put(targetClass, aspectList);
                }
            }
        }
        return targetMap;
    }
}
