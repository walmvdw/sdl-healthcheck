
cd ../

If (Test-Path './config/logback.xml' -PathType Leaf) {
    Start-Process java -ArgumentList '-Dlogging.config=./config/logback.xml', '-jar', '${jarName}'
}
Else {
    Start-Process java -ArgumentList '-jar', '${jarName}'
}

echo "Started Service Healthceck"