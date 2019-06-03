
Handler 架构图


![](https://i.imgur.com/C5lHvoM.jpg)


一个APP钟运行多个线程，不同线程可以相互拿Handler对象  
MessageQueue和native直接通信，native又和kernel通信。  
epoll机制在kernel中维护了一个链表和一棵红黑树使它的效率优于poll和select


