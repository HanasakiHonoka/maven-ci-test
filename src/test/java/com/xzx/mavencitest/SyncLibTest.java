package com.xzx.mavencitest;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.SystemUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Classname SyncLibTest
 * @Description
 * @Date 2022/10/14 21:56
 * @Author XZX
 * @Version 1.0
 */
public class SyncLibTest {
    private static Session session;
    private static ChannelSftp channelSftp;
    //只打印日志，不做上传删除操作（debug用）
    private static final Boolean noDebug = true;


    //cd 到服务器上的jar包所在目录
    private static final String serverLibPath = "/home/mvnci/lib";
    //这个是intellij idea中分离打包后的lib的路径, 大家可以直接改成自己本地的绝对路径
    public static final String newLibPath = SystemUtil.get(SystemUtil.USER_DIR) + "/lib/";
    public static final String user = "root";
    public static final String ip = "47.241.246.150";

    public static final String privateKeyPath = SystemUtil.get(SystemUtil.USER_HOME) + "\\.ssh\\id_old";

    @Test
    public void testMain() throws Exception {

        //初始化ssh连接
        initChannel();
        try {

            channelSftp.cd(serverLibPath);
            //将jar目录下的jar都ls出来，保存到oldJarList中
            Vector ls = channelSftp.ls(".");
            List<String> oldJarList = new ArrayList<>();
            for (int i = 0; i < ls.size(); i++) {
                ChannelSftp.LsEntry o = (ChannelSftp.LsEntry) ls.get(i);
                String filename = o.getFilename();
                if (filename.endsWith(".jar")) {
                    oldJarList.add(filename);
                }
            }
            //获取本地最新jar的列表
            List<String> newLibList = FileUtil.listFileNames(newLibPath);

            //对比新老jar列表，找出要新增的jar,删除的jar
            List<String> addJarList = addJarList(oldJarList, newLibList);
            List<String> removeJarList = removeJarList(oldJarList, newLibList);

            //上传新增的jar
            for (String addJarName : addJarList) {
                if (noDebug) {
                    channelSftp.put(newLibPath + addJarName, addJarName);
                }
                System.out.println("add jar ==> " + addJarName);
            }

            //删除过期的jar
            for (String removeJarName : removeJarList) {
                if (noDebug) {
                    channelSftp.rm(removeJarName);
                }
                System.out.println("remove jar ==> " + removeJarName);
            }
        } finally {
            //关闭ssh连接
            closeChannel();
        }

    }

    public static List<String> removeJarList(List<String> oldLibList, List<String> newLibList) {
        Set<String> newLibSet = new HashSet<>(newLibList);
        return oldLibList.stream().filter(item -> !newLibSet.contains(item)).collect(Collectors.toList());
    }

    public static List<String> addJarList(List<String> oldLibList, List<String> newLibList) {
        Set<String> oldLigSet = new HashSet<>(oldLibList);
        return newLibList.stream().filter(item -> !oldLigSet.contains(item)).collect(Collectors.toList());
    }

    public static void initChannel() throws JSchException {
        //声明JSCH对象
        JSch jSch = new JSch();
        //获取一个Linux会话
        //设置公钥
        jSch.addIdentity(privateKeyPath);
        session = jSch.getSession(user, ip, 22);
        session.setConfig("PreferredAuthentications", "publickey");
        ////设置登录密码
        //session.setPassword("");
        //关闭key的检验
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        session.setConfig(sshConfig);
        //连接Linux
        session.connect();
        //通过sftp的方式连接
        channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
    }

    private static void closeChannel() {
        if (channelSftp != null) {
            channelSftp.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }
}
