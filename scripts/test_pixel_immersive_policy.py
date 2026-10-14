#!/usr/bin/env python3
"""Compile and execute the actual dependency-free Kotlin inset policy."""
from pathlib import Path
import os, shutil, subprocess, tempfile
root=Path(__file__).resolve().parent.parent
src=root/'app/src/main/java/io/fluxreader/app/ui/ImmersiveLayoutPolicy.kt'
compiler=shutil.which('kotlinc')
if compiler is None:
 print('SKIP: standalone Kotlin policy check requires kotlinc')
 raise SystemExit(0)
with tempfile.TemporaryDirectory() as tmp:
 tmp=Path(tmp)
 (tmp/'PolicyTest.kt').write_text('''package io.fluxreader.app.ui
fun main() {
  check(ImmersiveLayoutPolicy.reserveSystemInsets(false))
  check(!ImmersiveLayoutPolicy.reserveSystemInsets(true))
  check(ImmersiveLayoutPolicy.cameraGuardDp(0f) == 22f)
  check(ImmersiveLayoutPolicy.cameraGuardDp(27f) == 27f)
  check(ImmersiveLayoutPolicy.cameraGuardDp(-42f) == 22f)
  check(ImmersiveLayoutPolicy.contentTopDp(false, 27f, 52f) == 52f)
  check(ImmersiveLayoutPolicy.contentTopDp(true, 27f, 52f) == 27f)
  check(ImmersiveLayoutPolicy.contentTopDp(true, 0f, 52f) == 22f)
  println("Pixel immersive Kotlin geometry checks: 8/8 PASS")
}
''')
 jar=tmp/'test.jar'
 subprocess.run([compiler,str(src),str(tmp/'PolicyTest.kt'),'-include-runtime','-d',str(jar)],check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
 subprocess.run(['java','-jar',str(jar)],check=True)
