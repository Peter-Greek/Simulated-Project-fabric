"""Runs the mixin audit: resolves the dev classpath, builds the tool, checks the mixins.

    python tools/mixin_audit/run.py

Exits non-zero when a mixin does not match the 1.20.1 class it targets. Needs the
Gradle dependency cache populated, so run a build first.
"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MIXIN_CLASSES = ROOT / 'simulated/fabric/build/classes/java/main/dev/simulated_team/simulated/mixin'
GRADLE = str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew'))

# Loom does not expose the dev classpath as a task, so ask for it through an init
# script rather than guessing at cache layout.
INIT_SCRIPT = """
allprojects {
    tasks.register('printMixinAuditCp') {
        doLast {
            def cp = project.configurations.findByName('compileClasspath')
            if (cp != null) { cp.resolve().each { println "CPJAR:" + it.absolutePath } }
        }
    }
}
"""


def run(cmd, **kwargs):
    return subprocess.run(cmd, cwd=ROOT, text=True, **kwargs)


def main():
    if not MIXIN_CLASSES.is_dir():
        print('compiled mixins not found; run the build first:')
        print('  ./gradlew :simulated:fabric:compileJava')
        return 2

    work = Path(tempfile.mkdtemp(prefix='mixin-audit-'))
    try:
        init = work / 'cp.gradle'
        init.write_text(INIT_SCRIPT, encoding='utf-8')

        proc = run([GRADLE, '-I', str(init), ':simulated:fabric:printMixinAuditCp', '-q', '--console=plain'],
                   stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
        jars = [line[len('CPJAR:'):] for line in proc.stdout.splitlines() if line.startswith('CPJAR:')]
        if not jars:
            print('could not resolve the compile classpath')
            return 2

        classpath_file = work / 'classpath.txt'
        classpath_file.write_text('\n'.join(jars), encoding='utf-8')

        asm = [j for j in jars if '/asm' in j.replace('\\', '/').lower() or '\\asm' in j.lower()]
        if not asm:
            print('ASM is not on the compile classpath')
            return 2
        sep = ';' if os.name == 'nt' else ':'
        asm_cp = sep.join(asm)

        out = work / 'classes'
        out.mkdir()
        compile_proc = run(['javac', '-cp', asm_cp, '-d', str(out),
                            str(ROOT / 'tools/mixin_audit/MixinAudit.java')],
                           stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if compile_proc.returncode != 0:
            print(compile_proc.stdout)
            return 2

        audit = run(['java', '-cp', str(out) + sep + asm_cp, 'tools.mixin_audit.MixinAudit',
                     str(MIXIN_CLASSES), str(classpath_file)])
        return audit.returncode
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == '__main__':
    sys.exit(main())
