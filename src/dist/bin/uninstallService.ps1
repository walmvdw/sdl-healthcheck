# Script provided by Mihai Cădariu - http://yatb.mitza.net/2017/09/sdl-healthcheck-as-windows-service.html

function waitServiceStop {
    param (
        [string]$svcName
    )
    $svc = Get-Service $svcName
    # Wait for 30s
    $svc.WaitForStatus('Stopped', '00:00:30')
    if ($svc.Status -ne 'Stopped') {
        $Host.UI.WriteErrorLine("ERROR: Not able to stop service " + $serviceName)
    } else {
        Write-Host "Service '$serviceName' is stopped." -ForegroundColor Green
    }
}


# Check script is launched with Administrator permissions
$isAdministrator = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")
if ($isAdministrator -eq $False) {
    $Host.UI.WriteErrorLine("ERROR: Please ensure script is launched with Administrator rights")
    Exit
}

$currentFolder = Get-Location
$defaultServiceName="SDLWebHealthcheck"

try {
    $scriptPath=$PSScriptRoot
    cd $scriptPath\.. 
    $rootFolder = Get-Location

    $serviceName = $defaultServiceName

    if (-Not (Get-Service $serviceName -ErrorAction SilentlyContinue)) {
        $Host.UI.WriteErrorLine("ERROR: There is no service with name " + $serviceName)
        Exit
    }

    Write-Host "Stopping service '$serviceName'..." -ForegroundColor Green
    & sc.exe stop $serviceName
    waitServiceStop $serviceName

    Write-Host "Removing service '$serviceName'..." -ForegroundColor Green
    & sc.exe delete $serviceName
    if (-Not (Get-Service $serviceName -ErrorAction SilentlyContinue)) {
        Write-Host "Service '$serviceName' successfully removed." -ForegroundColor Green
    }
} Finally {
    cd $currentFolder
}
