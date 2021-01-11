学习笔记

# 作业1
自定义一个 Classloader，加载一个 Hello.xlass 文件，执行 hello 方法，
此文件内容是一个 Hello.class 文件所有字节（x=255-x）处理后的文件

# 思路：
使用字节流读取xlass文件，加载类，得到类的所有自有方法并执行。