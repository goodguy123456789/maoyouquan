关闭步骤如下：

1. 停止 cpolar（断开公网访问）


sc stop cpolar
2. 停止 Spring Boot 后端

任务管理器 → 找到占用内存最大的 java.exe → 右键结束任务

或命令行：


# 查找8088端口的进程ID
netstat -ano | findstr :8088
# 结束该进程（把PID替换成实际数字）
taskkill /PID 替换为实际PID /F
3. 停止 Nginx


D:\nginx-1.22.0-web\nginx-1.22.0-web\nginx.exe -s stop
下次重新开启时：


# 启动 Nginx
D:\nginx-1.22.0-web\nginx-1.22.0-web\nginx.exe

# 启动 Spring Boot（在项目目录下）
java -jar C:\Users\Administrator\Desktop\demo\backend\target\maoyouquan-0.0.1-SNAPSHOT.jar

# 启动 cpolar
sc start cpolar
cpolar 服务是开机自启的，重启电脑后会自动启动，通常不需要手动操作。