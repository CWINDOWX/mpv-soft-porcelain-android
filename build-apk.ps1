param(
    [ValidateSet("release", "preview", "debug")]
    [string]$Variant = "preview"
)

if (Test-Path "E:\Android\jdk-21") { $env:JAVA_HOME = "E:\Android\jdk-21" }
if (-not $env:ANDROID_HOME -and (Test-Path "E:\Android\Sdk")) { $env:ANDROID_HOME = "E:\Android\Sdk" }
if (-not $env:ANDROID_SDK_ROOT -and $env:ANDROID_HOME) { $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME }
if (-not $env:GRADLE_USER_HOME -and (Test-Path "E:\Android\.gradle")) { $env:GRADLE_USER_HOME = "E:\Android\.gradle" }
if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }

$task = switch ($Variant) {
    "release" { ":app:assembleRelease" }
    "debug"   { ":app:assembleDebug" }
    default   { ":app:assemblePreview" }
}
Push-Location $PSScriptRoot
try {
    & cmd /c "gradlew.bat $task --max-workers=4 --console=plain"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $outDir = Join-Path $PSScriptRoot "app\build\outputs\apk\$Variant"
    $apk = Get-ChildItem $outDir -Filter "*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($apk -and ($Variant -eq "preview" -or $Variant -eq "release")) {
        $distDir = Join-Path $PSScriptRoot "dist"
        New-Item -ItemType Directory -Force $distDir | Out-Null
        $dest = Join-Path $distDir "Porcelain-MPV-arm64.apk"
        Copy-Item -Force $apk.FullName $dest
        Write-Output "Copied APK -> $dest"
    }
    Get-ChildItem $outDir -Filter "*.apk" | Select-Object FullName, @{N="MB";E={[math]::Round($_.Length/1MB, 2)}}
} finally {
    Pop-Location
}
