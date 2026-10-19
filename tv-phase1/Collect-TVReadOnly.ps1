param(
    [Parameter(Mandatory=$true)][string]$AdbPath,
    [Parameter(Mandatory=$true)][string]$Device
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) { throw 'ADB executable not found.' }
$AdbPath = (Resolve-Path -LiteralPath $AdbPath).Path
$state = & $AdbPath -s $Device get-state 2>&1
if ($LASTEXITCODE -ne 0 -or ($state -join '').Trim() -ne 'device') {
    throw 'Selected TV is not connected and authorized. No diagnostics collected.'
}
$report = Join-Path $PSScriptRoot ('reports/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $report -Force | Out-Null
function Save-Read {
    param([string]$Name, [string[]]$Arguments)
    $output = & $AdbPath -s $Device @Arguments 2>&1
    $code = $LASTEXITCODE
    $lines = @(
        ('Command: adb -s <selected-TV> ' + ($Arguments -join ' '))
        ('Exit code: ' + $code)
        ''
    )
    $lines += @($output | ForEach-Object { [string]$_ })
    $lines |
        Out-File -LiteralPath (Join-Path $report ($Name + '.txt')) -Encoding utf8
}
$properties = @('ro.product.manufacturer','ro.product.model','ro.product.name','ro.product.device',
    'ro.build.version.release','ro.build.version.sdk','ro.build.version.security_patch',
    'ro.build.display.id','ro.build.id','ro.build.version.incremental','ro.build.fingerprint')
foreach ($property in $properties) { Save-Read $property @('shell','getprop',$property) }
Save-Read 'current-user' @('shell','am','get-current-user')
Save-Read 'packages' @('shell','pm','list','packages','-f','--user','current')
Save-Read 'packages-system' @('shell','pm','list','packages','-s','--user','current')
Save-Read 'packages-third-party' @('shell','pm','list','packages','-3','--user','current')
Save-Read 'packages-disabled' @('shell','pm','list','packages','-d','--user','current')
Save-Read 'storage' @('shell','df','-h','/data','/storage/emulated/0')
Save-Read 'memory-kernel' @('shell','cat','/proc/meminfo')
Save-Read 'memory-processes' @('shell','dumpsys','meminfo')
Save-Read 'cpu-snapshot' @('shell','dumpsys','cpuinfo')
Save-Read 'uptime' @('shell','uptime')
Save-Read 'launcher-default' @('shell','cmd','package','resolve-activity','--brief','--user','current','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
Save-Read 'launcher-candidates' @('shell','cmd','package','query-activities','--brief','--user','current','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
Write-Output "Read-only reports saved to: $report"
