@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_EXEC=C:\Users\User\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"

if exist "%GRADLE_EXEC%" (
    call "%GRADLE_EXEC%" %*
) else (
    call build_apk.bat %*
)
