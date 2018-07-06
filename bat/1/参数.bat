@echo off

:: set var=请输入一个变量

:: echo %var%
:: title %var%

:: set /p var2=%var%


:: echo %var2%



:: call :loop Hello World!

:: pause>nul & goto :eof

:: :loop

:: echo %1

:: echo %2


@echo off

title 阶乘--递归算法

echo 阶乘--递归算法

::setlocal enabledelayedexpansion



::set /p n=请输入一个数 ：

::set result=1

::if !n!==0 (echo 结果等于 1 & pause>nul & goto eof) else (call :loop !n!)

::echo. & echo 结果等于!result!

::pause>nul

:::loop

::if not %1==1 (

::set /a result=!result!*%1

::set /a x=%1

::set /a x-=1

::call :loop !x!

::)



setlocal enabledelayedexpansion

set num=12

for /l %%i in (1,1,5) do (

set /a num+=10

echo !num!

)

pause
