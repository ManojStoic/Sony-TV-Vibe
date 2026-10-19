param(
    [Parameter(Mandatory=$true)][string]$Device,
    [Parameter(Mandatory=$true)][string[]]$Package
)
$ErrorActionPreference = 'Stop'
$adb = (Resolve-Path "$PSScriptRoot/../tv-phase1/platform-tools/adb.exe").Path
$report = Join-Path $PSScriptRoot ('../tv-phase1/reports/launch-targets-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Force $report | Out-Null
function Save-Read([string]$Name,[string[]]$Arguments) {
    $old = $ErrorActionPreference
    try {$ErrorActionPreference='Continue';$result=& $adb -s $Device @Arguments 2>&1;$code=$LASTEXITCODE}
    finally {$ErrorActionPreference=$old}
    @('Command: adb -s <selected-TV> '+($Arguments -join ' '),"Exit code: $code",'') + @($result | ForEach-Object {"$_"}) | Set-Content (Join-Path $report "$Name.txt") -Encoding utf8
    return $code
}
if((Save-Read 'connection' @('get-state')) -ne 0){throw 'Selected device is unavailable. The failed read was recorded.'}
[void](Save-Read 'device-properties' @('shell','getprop'))
foreach($pkg in $Package){
    $safe=$pkg -replace '[^A-Za-z0-9._-]','_'
    [void](Save-Read "$safe-details" @('shell','dumpsys','package',$pkg))
    foreach($category in @('LEANBACK_LAUNCHER','LAUNCHER')){
        [void](Save-Read "$safe-$category" @('shell','cmd','package','query-activities','--brief','--user','current','-a','android.intent.action.MAIN','-c',"android.intent.category.$category",'-p',$pkg))
    }
}
Write-Output "Read-only target reports saved to: $report"
