#!/usr/bin/env python3
"""Materialize exactly the Android and backend preparation steps used in CI."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys

import yaml

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("destination", type=Path)
args = parser.parse_args()
repo = Path(__file__).resolve().parents[1]
target = args.destination.resolve()
target.mkdir(parents=True, exist_ok=True)
if any(target.iterdir()):
    parser.error("destination must be empty")

for directory in ("updates", "deploy", "tools"):
    shutil.copytree(repo / directory, target / directory, ignore=shutil.ignore_patterns("__pycache__"))
shutil.copy2(repo / "MLBB_ASSISTANT_CODIGO_GITHUB.zip", target)
workflow = yaml.safe_load((repo / ".github/workflows/development-v023.yml").read_text())
steps = workflow["jobs"]["apk"]["steps"]
for step in steps[1:]:
    if step["name"] == "Configurar Java 17":
        break
    print(step["name"], flush=True)
    env = dict(os.environ, PATH=str(Path(sys.executable).parent) + os.pathsep + os.environ["PATH"])
    env.update(step.get("env", {}))
    subprocess.run(["bash", "-eo", "pipefail", "-c", step["run"]], cwd=target, env=env, check=True)
