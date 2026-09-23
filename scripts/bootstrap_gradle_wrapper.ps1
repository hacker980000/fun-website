$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Version = "9.6.0"
$ExpectedSha = "497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
$Jar = Join-Path $Root "gradle\wrapper\gradle-wrapper.jar"
$Url = "https://raw.githubusercontent.com/gradle/gradle/v$Version/gradle/wrapper/gradle-wrapper.jar"

function Test-WrapperJar([string]$Path) {
    if (-not (Test-Path $Path)) { return $false }
    $Actual = (Get-FileHash $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($Actual -ne $ExpectedSha) {
        throw "Wrapper JAR checksum mismatch. Expected $ExpectedSha, got $Actual"
    }
    return $true
}

if (Test-Path $Jar) {
    if (Test-WrapperJar $Jar) {
        Write-Host "Verified Gradle wrapper JAR already present."
        exit 0
    }
}

New-Item -ItemType Directory -Force -Path (Split-Path $Jar) | Out-Null
$TempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("gradle-wrapper-bootstrap-" + [guid]::NewGuid().ToString())
New-Item -ItemType Directory -Force -Path $TempRoot | Out-Null
$Candidate = Join-Path $TempRoot "gradle-wrapper.jar"

try {
    $Downloaded = $false
    try {
        Invoke-WebRequest -Uri $Url -OutFile $Candidate -UseBasicParsing
        if (Test-WrapperJar $Candidate) {
            Copy-Item $Candidate $Jar -Force
            Write-Host "Gradle wrapper JAR downloaded from the official Gradle repository and verified."
            $Downloaded = $true
        }
    } catch {
        Write-Warning "Verified wrapper download was unavailable; trying local Gradle fallback."
        Remove-Item $Candidate -Force -ErrorAction SilentlyContinue
    }
    if ($Downloaded) { exit 0 }

    $GradleCommand = Get-Command gradle -ErrorAction SilentlyContinue
    if ($null -ne $GradleCommand) {
        $VersionText = (& gradle --version | Select-String '^Gradle ' | Select-Object -First 1).ToString()
        $Installed = ($VersionText -replace '^Gradle\s+', '').Trim()
        if ($Installed -eq $Version) {
            Set-Content -Path (Join-Path $TempRoot "settings.gradle.kts") -Value 'rootProject.name = "wrapper-bootstrap"' -Encoding UTF8
            Set-Content -Path (Join-Path $TempRoot "build.gradle.kts") -Value '' -Encoding UTF8
            & gradle -p $TempRoot --no-daemon wrapper --gradle-version $Version --distribution-type bin
            if ($LASTEXITCODE -ne 0) { throw "Local Gradle wrapper generation failed." }
            $Generated = Join-Path $TempRoot "gradle\wrapper\gradle-wrapper.jar"
            if (Test-WrapperJar $Generated) {
                Copy-Item $Generated $Jar -Force
                Write-Host "Gradle wrapper JAR generated locally and verified."
                exit 0
            }
        } else {
            Write-Warning "Installed Gradle is $Installed; Gradle $Version is required for local generation."
        }
    }

    throw "Unable to restore the verified Gradle wrapper JAR. Allow access to raw.githubusercontent.com or install Gradle $Version and rerun this script."
} finally {
    Remove-Item $TempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
