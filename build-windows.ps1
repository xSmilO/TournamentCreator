$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME) {
    throw 'Set JAVA_HOME to a Windows JDK 22 or newer.'
}
$jpackage = Join-Path $env:JAVA_HOME 'bin/jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
    throw 'jpackage.exe was not found. Set JAVA_HOME to a Windows JDK 22 or newer.'
}

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd clean package '-Pwindows'
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

    New-Item -ItemType Directory -Path target/windows | Out-Null
    & $jpackage --type app-image --name TournamentCreator --dest target/windows `
        --input target/windows-input --main-jar TournamentCreator.jar `
        --main-class com.hltv.tournamentcreator.Launcher `
        --java-options '-Dtournamentcreator.configDir=$APPDIR/..'
    if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed.' }

    Write-Output (Join-Path $PSScriptRoot 'target/windows/TournamentCreator/TournamentCreator.exe')
} finally {
    Pop-Location
}
