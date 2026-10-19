"""Bounded test helper for this prototype. Every device action needs an explicit target."""
from argparse import ArgumentParser
from pathlib import Path
import datetime, subprocess, sys

ROOT=Path(__file__).resolve().parent.parent
parser=ArgumentParser()
parser.add_argument('--device',required=True,help='Authorized ADB serial or host:port')
parser.add_argument('--adb',default=str(ROOT/'tv-phase1/platform-tools/adb.exe'))
parser.add_argument('--report-dir',default=str(ROOT/'tv-phase1/reports'/('native-test-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))))
parser.add_argument('action',choices=('install','launch','capture'))
parser.add_argument('name',nargs='?',help='Required capture name')
args=parser.parse_args()
adb=Path(args.adb); report=Path(args.report_dir); report.mkdir(parents=True,exist_ok=True)
if not adb.is_file(): raise SystemExit('ADB executable not found; install official Platform-Tools first.')
if args.action=='capture' and (not args.name or not args.name.replace('-','').isalnum()): raise SystemExit('Capture name must use letters, digits, or hyphens.')
def run(*parts,binary=False):
    result=subprocess.run([str(adb),'-s',args.device,*parts],capture_output=True,timeout=90)
    with (report/'commands.log').open('a',encoding='utf-8') as out:
        out.write(f'{datetime.datetime.now().isoformat()} adb -s <selected-TV> {parts!r}: exit={result.returncode}\n')
        if not binary: out.write(result.stdout.decode(errors='replace')+result.stderr.decode(errors='replace')+'\n')
    if result.returncode: raise SystemExit(result.stderr.decode(errors='replace'))
    return result.stdout
if run('get-state').decode().strip()!='device': raise SystemExit('Selected device is not authorized.')
if args.action=='install': print(run('install','-r',str(ROOT/'tv-phase2/artifacts/tv-vibe-debug.apk')).decode())
elif args.action=='launch': print(run('shell','am','start','-W','-n','com.sonytvvibe.launcher/.MainActivity').decode())
else:
    png=run('exec-out','screencap','-p',binary=True)
    if not png.startswith(b'\x89PNG\r\n\x1a\n'): raise SystemExit('Device did not return a PNG screenshot.')
    (report/(args.name+'.png')).write_bytes(png); print(report/(args.name+'.png'))
