@echo off
@rem SICOI Mobile Build Script - Updated 2026-07-26 17:42










set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_EXEC=C:\Users\User\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"

echo ===================================================
echo   Limpando e Compilando SICOI Mobile APK...
echo ===================================================

call "%GRADLE_EXEC%" clean assembleDebug --stacktrace > build_log.txt 2>&1


echo.
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ===================================================
    echo  [OK] APK GERADO COM SUCESSO!
    echo  Caminho do arquivo:
    echo  %CD%\app\build\outputs\apk\debug\app-debug.apk
    echo ===================================================
) else (
    echo ===================================================
    echo  [ERRO] Ocorreu uma falha na geracao do APK.
    echo ===================================================
    echo  Resumo da falha de compilação:
    echo ---------------------------------------------------
    powershell -Command "Select-String -Path build_log.txt -Pattern ':app:hiltJavaCompileDebug FAILED', 'error:' -Context 2,10 | ForEach-Object { $_.Context.PreContext; $_.Line; $_.Context.PostContext }"
    echo ---------------------------------------------------




)
echo.
pause


