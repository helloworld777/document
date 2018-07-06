扇形菜单

之前公司的项目做了一个扇形菜单，但是做的不太理想，又没时间修正。现在重构项目了，决心把它修正，当认真分析思路之后，发现其实还是挺简单的。<br>

![avatar](/S80801-14543748.gif)

这个是之前项目中实现的，发现三个扇形的菜单并不是刚好从中间那个加号中发散出去的。回来也不是刚好回到中间那个加号的按钮位置。

![avatar](/20180802152429.png)


首先先大概画出这些控件的位置。如果v控件从cicle控件中通过平移和旋转到v自己的位置，则要找到cicle的左上角的位置。<br>
这里先计算第一个v和cicle的距离，其他的v算法都是一样的<br>
先计算x轴方向的距离，要计算v和cicle控件x轴的位置，就是计算1线到2线之间的距离，<br>
由于cicle在buttom_layout里面的，因此bottom_layout.getLeft()+cicle.getLeft()的距离就是ciclr左边距离屏幕左边的距离，<br>
而线1距离左边屏幕的距离就是v.getRight()的距离<br>
bottom_layout.getLeft()+cicle.getLeft()-v.getRight()就是线1到线4的距离，最后再加上cicle的宽度，则最终是线1到线2的距离了<br>
再计算y轴方向的距离.同样是线5到线3的距离<br>
首先bottom_layout.getTop()+cicle.getTop()就是线6距离顶部的位置，<br>
然后再减去线5距离顶部的距离，也就是v.getBottom()的距离，就是线5和线6直接的距离<br>
再加上cicle.getHeight()的高度。则就是线5到线3的距离

调整后的效果图

![avatar](/abv.gif)



demo效果图
![avatar](/Screenrecorder-2018-08-01-14-50-11-988.gif)


demo地址https://github.com/helloworld777/hello-jni