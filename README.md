# ChatRoom
基于Java Socket的多人多房间终端聊天室，包含服务器和客户端。

## 项目功能
- 其他功能看项目
- 增加文件传输功能


## Todo
- 主线程必须创建房间，第二个线程来发送文件
- 发送文件的客户端发送完毕，服务端可以正常传输业务流，但是不能正常接收信息，怀疑是阻塞问题，
- 可以设置byte后面的整数，方便传输大文件，默认为4吧
## 启动
### 1:启动服务器
- 根据实际情况修改 *resources/server.properties* 下的配置文件
- 运行文件 *main/ServerStart.java*
### 2:启动客户端
- 根据实际情况修改 *resources/client.properties* 下的配置文件
- 运行文件 *main/ClientStart.java* (运行多次则启动多个客户端)



