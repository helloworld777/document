@echo off
for %%i in (%0) do (set "name=%%~ni") 
javap -verbose %name%.class > %name%.txt