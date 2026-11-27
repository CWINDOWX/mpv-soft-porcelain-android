param(
    [ValidateSet("preview", "debug")]
    [string]$Variant = "preview"
)

$env:JAVA_HOME = "E:\Android\jdk-21"
$env:PATH = "E:\Android\jdk-21\bin;$env:PATH"
$env:ANDROID_HOME = "E:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "E:\Android\Sdk"
$env:GRADLE_USER_HOME = "E:\Android\.gradle"

$task = if ($Variant -eq "debug") { ":app:assembleDebug" } else { ":app:assemblePreview" }
Push-Location $PSScriptRoot
try {
    & cmd /c "gradlew.bat $task --no-daemon --max-workers=4"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $outDir = Join-Path $PSScriptRoot "app\build\outputs\apk\$Variant"
    Get-ChildItem $outDir -Filter "*.apk" | Select-Object FullName, @{N="MB";E={[math]::Round($_.Length/1MB, 2)}}
} finally {
    Pop-Location
}
