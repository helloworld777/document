


@echo off

set title=ясЁы╠Да©
title %title%
echo %title%

setlocal enabledelayedexpansion

set num=12

for /l %%i in (1,1,5) do (

set /a num+=10

echo !num!

)

pause
