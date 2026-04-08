# Download Gradle wrapper on first run
# Run from project root: .\setup-gradle.ps1

$wrapperDir = "gradle\wrapper"
$wrapperJar = "$wrapperDir\gradle-wrapper.jar"

# Create wrapper directory
if (!(Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

# Download gradle-wrapper.jar if it doesn't exist
if (!(Test-Path $wrapperJar)) {
    Write-Host "Downloading gradle-wrapper.jar..."
    $url = "https://github.com/gradle/gradle/releases/download/v8.1.1/gradle-8.1.1-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $url -OutFile $wrapperJar -ErrorAction Stop
        Write-Host "Downloaded successfully!"
    } catch {
        Write-Host "Download failed. Using Maven Central mirror..."
        $url = "https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.1.1/gradle-wrapper-8.1.1.jar"
        Invoke-WebRequest -Uri $url -OutFile $wrapperJar
        Write-Host "Downloaded successfully!"
    }
}

Write-Host "Gradle wrapper setup complete. You can now run: .\gradlew.bat assembleDebug"
