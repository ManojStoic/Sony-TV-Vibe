param([string]$JdkPath = 'C:\Program Files\Java\jdk-17')
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$env:JAVA_HOME = $JdkPath
$env:PATH = "$JdkPath\bin;" + $env:PATH
$env:ANDROID_USER_HOME = Join-Path $project '.android'
$tools = Join-Path $project 'tools/sdk/build-tools/android-15'
$android = Join-Path $project 'tools/sdk/platform/android-35/android.jar'
if (!(Test-Path "$tools/aapt2.exe") -or !(Test-Path $android)) { throw 'Extract the verified SDK packages first; see README.md.' }
$build = Join-Path $project ('build/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
$reports = Join-Path $project '../tv-phase1/reports/builds'
New-Item -ItemType Directory -Force $build,"$build/gen","$build/classes","$build/dex",$reports,"$project/artifacts","$project/.android" | Out-Null
function Run([string]$Exe,[string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed with exit code $LASTEXITCODE" }
}
Start-Transcript -Path "$build/build.log" | Out-Null
try {
    Run "$tools/aapt2.exe" @('compile','--dir',"$project/app/src/main/res",'-o',"$build/resources.zip")
    Run "$tools/aapt2.exe" @('link','-o',"$build/base.apk",'-I',$android,'--manifest',"$project/app/src/main/AndroidManifest.xml",'--java',"$build/gen",'--auto-add-overlay',"$build/resources.zip")
    $sources = @(Get-ChildItem "$project/app/src/main/java","$build/gen" -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
    Run "$JdkPath/bin/javac.exe" (@('-encoding','UTF-8','--release','8','-classpath',$android,'-d',"$build/classes") + $sources)
    Run "$JdkPath/bin/jar.exe" @('cf',"$build/classes.jar",'-C',"$build/classes",'.')
    Run "$tools/d8.bat" @('--debug','--min-api','26','--lib',$android,'--output',"$build/dex","$build/classes.jar")
    Copy-Item "$build/base.apk" "$build/unsigned.apk"
    Run "$JdkPath/bin/jar.exe" @('uf',"$build/unsigned.apk",'-C',"$build/dex",'classes.dex')
    Run "$tools/zipalign.exe" @('-f','-p','4',"$build/unsigned.apk","$build/aligned.apk")
    $key = "$project/.android/debug.keystore"
    if (!(Test-Path $key)) {
        Run "$JdkPath/bin/keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-alias','androiddebugkey','-keypass','android','-dname','CN=Android Debug,O=Local Prototype,C=US','-keyalg','RSA','-keysize','2048','-validity','10000')
    }
    $apk = "$project/artifacts/tv-vibe-debug.apk"
    Run "$tools/apksigner.bat" @('sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,"$build/aligned.apk")
    Run "$tools/apksigner.bat" @('verify','--verbose','--print-certs',$apk)
    Run "$tools/zipalign.exe" @('-c','-v','4',$apk)
    Run "$tools/aapt2.exe" @('dump','badging',$apk)
    Get-FileHash $apk -Algorithm SHA256 | Format-List
} finally {
    Stop-Transcript | Out-Null
    Copy-Item "$build/build.log" "$reports/build-$([IO.Path]::GetFileName($build)).log"
}
