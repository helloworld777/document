##一、启动电源以及系统启动

当电源按下时引导芯片代码会从预定义的地方（固化在ROM）开始执行，加载引导程序BootLoader到RAM，然后执行。

##二、引导程序BootLoader

它是Android操作系统开始运行前的一个小程序，主要将操作系统OS拉起来并进行。

##三、Linux内核启动

当内核启动时，设置缓存、被保护存储器、计划列表、加载驱动。此外，还启动了Kernel的swapper进程（pid = 0）和kthreadd进程（pid = 2）。下面分别介绍下它们：

    swapper进程：又称为idle进程，系统初始化过程Kernel由无到有开创的第一个进程, 用于初始化进程管理、内存管理，加载Binder Driver、Display、Camera Driver等相关工作。
    kthreadd进程：Linux系统的内核进程，是所有内核进程的鼻祖，会创建内核工作线程kworkder，软中断线程ksoftirqd，thermal等内核守护进程。

当内核完成系统设置时，它首先在系统文件中寻找init.rc文件，并启动init进程。
##四、init进程启动

init进程主要用来初始化和启动属性服务，并且启动Zygote进程。

###1、init进程是什么？

Linux系统的用户进程，是所有用户进程的鼻祖，进程号为1，它有许多重要的职责，比如创建Zygote孵化器和属性服务等。并且它是由多个源文件组成的，对应源码目录system/core/init中。