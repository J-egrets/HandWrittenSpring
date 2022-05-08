package cn.egret.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类操作工具类
 * @author egret
 */
public final class ClassUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);

    /**
     * 获取类加载器
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 加载类
     *
     * @param className     类名
     * @param isInitialized 是否初始化
     * @return
     */
    public static Class<?> loadClass(String className, boolean isInitialized) {
        Class<?> cls;
        try {
            cls = Class.forName(className, isInitialized, getClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("load class failure", e);
            throw new RuntimeException(e);
        }
        return cls;
    }

    /**
     * 加载类（默认将初始化类）
     */
    public static Class<?> loadClass(String className) {
        return loadClass(className, true);
    }

    /**
     * 获取指定包名下的所有类
     * @param packageName 包名
     * @return
     */
    public static Set<Class<?>> getClassSet(String packageName) {
        Set<Class<?>> classSet = new HashSet<>();
        try {
            Enumeration<URL> urls = getClassLoader().getResources(packageName.replace(".", "/"));
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String protocol = url.getProtocol();
                    if (protocol.equals("file")) {
                        // file文件
                        // 把%20转换成空格，得到包路径
                        String packagePath = url.getPath().replaceAll("%20", " ");
                        // 添加类文件
                        addClass(classSet, packagePath, packageName);
                    } else if (protocol.equals("jar")) {
                        // jar文件
                        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                        if (jarURLConnection != null) {
                            // 获取jar
                            JarFile jarFile = jarURLConnection.getJarFile();
                            if (jarFile != null) {
                                // 从此jar包 得到一个枚举类
                                Enumeration<JarEntry> jarEntries = jarFile.entries();
                                // 进行循环迭代
                                while (jarEntries.hasMoreElements()) {
                                    // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                                    JarEntry jarEntry = jarEntries.nextElement();
                                    String jarEntryName = jarEntry.getName();
                                    // 如果是一个.class文件
                                    if (jarEntryName.endsWith(".class")) {
                                        String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                        // 添加到classSet容器中
                                        doAddClass(classSet, className);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("get class set failure", e);
            throw new RuntimeException(e);
        }
        return classSet;
    }

    /**
     * 添加类文件
     * @param classSet 类容器
     * @param packagePath 包路径
     * @param packageName 包名
     */
    private static void addClass(Set<Class<?>> classSet, String packagePath, String packageName) {
        File[] files = new File(packagePath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // 寻找目录或者class文件
                return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
            }
        });
        for (File file : files) {
            String fileName = file.getName();
            if (file.isFile()) {
                // file是文件
                // 参数: beginIndex – 起始索引，包括在内。 endIndex – 结束索引，独占。
                // 获取类名字
                String className = fileName.substring(0, fileName.lastIndexOf("."));
                if (StringUtils.isNotEmpty(packageName)) {
                    // 拼接出来完整类名
                    className = packageName + "." + className;
                }
                // 真实地添加类到容器中
                doAddClass(classSet, className);
            } else {
                // file是目录
                // 子包目录路径
                String subPackagePath = fileName;
                if (StringUtils.isNotEmpty(packagePath)) {
                    // 拼接出来完整子包目录路径
                    subPackagePath = packagePath + "/" + subPackagePath;
                }
                // 子包名字
                String subPackageName = fileName;
                if (StringUtils.isNotEmpty(packageName)) {
                    // 拼接出来完整子包名字
                    subPackageName = packageName + "." + subPackageName;
                }
                // 递归调用addClass，添加类文件，因为当前层是目录
                addClass(classSet, subPackagePath, subPackageName);
            }
        }
    }

    /**
     * 真实地添加类到容器中
     * @param classSet 类容器
     * @param className 类完整名字
     */
    private static void doAddClass(Set<Class<?>> classSet, String className) {
        // 不初始化类
        Class<?> cls = loadClass(className, false);
        // 将类文件加到容器中
        classSet.add(cls);
    }
}
