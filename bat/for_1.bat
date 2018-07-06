
@echo off
set title=for命令测试
title %title%
echo %title%

::仅为文件夹
echo ----------打印当前目录下文件夹名称---------
for /d %%i in (*) do @echo %%i
echo ----------打印当前目录下只有1-3个字符文件夹名称------------
for /d %%i in (???) do @echo %%i
echo ----------打印包含window字符文件夹名称------------
for /d %%i in (window?) do @echo %%i


::递归 
::进入根文件夹树 [Drive:]Path，在树的每一个文件夹中运行 for 语句。假设在 /R 后没有指定文件夹，则觉得是
::当前文件夹。假设 Set 仅仅是一个句点 (.)，则仅仅枚举文件夹树。
echo ----------打印包含bat字符文件夹名称------------
for /r %%i in (*.bat) do @echo %%i
::echo ----------打印g盘所有包含.zip字符文件夹名称------------
::for /r  %%i in (*.bat) do @echo %%i


::迭代数值范围 
::Start#,Step#,End#
echo ----------没有/L参数------------
for %%i in (1,2,5) do @echo %%i
echo ----------有/L参数------------
for /L %%i in (1,2,5) do @echo %%i


::打开5个cmd窗体
::for /l %%i in (1,1,5) do start cmd
echo ----------显示a.txt文件内容 type命令方法------------
type a.txt
echo ----------显示a.txt文件内容 /f 参数 for命令方法------------
for /f %%i in (a.txt) do echo %%i
echo ----------显示a.txt文件内容 没有/f 参数 for命令方法------------
for  %%i in (a.txt) do echo %%i

::delims 用来告诉for每一行应该拿什么作为分隔符，默认的分隔符是空格和tab键 ::=后面有一个空格，意思是再将每一个元素以空格切割，默认是仅仅取切割之后的第一个元素。
echo ----------delims 参数 for命令方法------------
for /f "delims= " %%i in (a.txt) do echo %%i

::它的作用就是当你通过delims将每一行分为更小的元素时，由它来控制要取哪一个或哪几个。
::同一时候tokens支持通配符*，以及限定范围。
::假设要显示第二列和第三列，则换成tokens=2,3或tokens=2-3,假设还有很多其它的则为：tokens=2-10之类的。
::由于你的tokens后面要取每一行的两列，用%%i来替换第二列，用%%j来替换第三列。
::而且必须是依照英文字母顺序排列的，%%j不能换成%%k，由于i后面是j
echo ----------tokens 参数 for命令方法------------
for /f "tokens=2-3 delims= " %%i in (a.txt) do echo %%i %%j

::对以通配符*，就是把这一行所有或者这一行的剩余部分当作一个元素了。
echo ----------tokens 2*参数 for命令方法------------
for /f "tokens=1,2* delims= " %%i in (a.txt) do echo %%i %%j %%k

::对以通配符*，就是把这一行所有或者这一行的剩余部分当作一个元素了。
echo ----------tokens 参数 for命令方法------------
for /f "tokens=* delims= " %%i in (a.txt) do echo %%i

::还有skip合eol，这俩个简单，skip就是要忽略文件的前多少行，而eol用来指定当一行以什么符号開始时，就忽略它。
echo ----------skip tokens 参数 for命令方法------------
for /f "skip=2 tokens=* " %%i in (a.txt) do echo %%i

::用eol来告诉for忽略以“.”开头的行。
echo ----------eol tokens 参数 for命令方法------------
for /f "eol=." %%i in (a.txt) do echo %%i

pause
