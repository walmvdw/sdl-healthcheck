# Script provided by Mihai Cădariu - http://yatb.mitza.net/2017/09/sdl-healthcheck-as-windows-service.html
# Java options and system properties to pass to the JVM when starting the service
# Separate double-quoted options with a comma. For example:
# $jvmoptions = "-Xrs", "-Xms512m", "-Xmx1024m", "-Dfile.encoding=UTF-8", "-Dmy.system.property='/a folder with a space in it/'"
$jvmoptions = "-Xrs", "-Xms48m", "-Xmx256m", "-Dfile.encoding=UTF-8"

$currentFolder = Get-Location
$name="SDLWebHealthcheck"
$displayName="SDL Web Healthcheck Service"
$description="SDL Web Healthcheck Service"
$serverPort="--server.port=8099"
$dependsOn=""
$path=$PSScriptRoot
cd $path\..
$rootFolder = Get-Location
$procrun="procrun.exe"
$application=$path + "\" + $procrun
$fullPath=$path + "\" + $procrun

$arguments = @()
$arguments += "//IS//" + $name
$arguments += "--DisplayName=" + $displayName
$arguments += "--Description=" + $description
$arguments += "--Install=" + $fullPath
$arguments += "--Jvm=auto"
$arguments += "--Startup=auto"
$arguments += "--LogLevel=Info"
$arguments += "--StartMode=jvm"
$arguments += "--StartPath=" + $rootFolder
$arguments += "--StartClass=org.springframework.boot.loader.JarLauncher"
$arguments += "--StartParams=start"
$arguments += "++StartParams=" + $serverPort
$arguments += "--StopMode=jvm"
$arguments += "--StopClass=java.lang.System"
$arguments += "--StopParams=exit"

$classpath = ".\bin\*;.\lib\*;.\addons\*;.\config"
foreach ($folder in Get-ChildItem -path $rootFolder -recurse | ?{ $_.PSIsContainer } | Resolve-Path -relative | Where { $_ -match 'services*' })
{
    $classpath = $folder + ";" + $folder + "\*;" + $classpath
}
$arguments += $jvmoptions | Foreach-Object{ '++JvmOptions=' + $_ }
$arguments += "--Classpath=" + $classpath

# Check script is launched with Administrator permissions
$isAdministrator = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")
if ($isAdministrator -eq $False) {
    $Host.UI.WriteErrorLine("ERROR: Please ensure script is launched with Administrator rights")
    Exit
}

Try {
    Write-Host "Installing '$name' as windows service..." -ForegroundColor Green
    if (Get-Service $name -ErrorAction SilentlyContinue) {
        Write-Warning "Service '$name' already exists in system."
    } else {
        & $application $arguments
        Start-Sleep -s 3

        if (Get-Service $name -ErrorAction SilentlyContinue) {
            Write-Host "Service '$name' successfully installed." -ForegroundColor Green
        } else {
            $Host.UI.WriteErrorLine("ERROR: Unable to create the service '" + $name + "'")
            Exit
        }
    }

    if ((Get-Service $name -ErrorAction SilentlyContinue).Status -ne "Running") {
        Write-Host "Starting service '$name'..." -ForegroundColor Green
        & sc.exe start $name
    } else {
        Write-Host "Service '$name' already started." -ForegroundColor Green
    }
} Finally {
    cd $currentFolder
}
