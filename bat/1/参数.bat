@echo off

:: set var=������һ������

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

title �׳�--�ݹ��㷨

echo �׳�--�ݹ��㷨

::setlocal enabledelayedexpansion



::set /p n=������һ���� ��

::set result=1

::if !n!==0 (echo ������� 1 & pause>nul & goto eof) else (call :loop !n!)

::echo. & echo �������!result!

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
