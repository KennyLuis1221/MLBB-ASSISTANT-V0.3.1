"""Apply recovery changes to the materialized V0.17 Android and backend trees."""
import argparse
from pathlib import Path
import shutil
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--server-only", action="store_true")
args = parser.parse_args()
update = Path(__file__).resolve().parent


def patch(name):
    commands = ["git", "apply", str(update / name)]
    subprocess.run(commands[:2] + ["--check"] + commands[2:], check=True)
    subprocess.run(commands, check=True)


patch("backend.patch")
if not args.server_only:
    patch("android.patch")
    package = "java/com/mlbb/assistant"
    for source, kind in (("main", "main"), ("test", "test")):
        destination = Path("android-app/app/src") / kind / package
        for file in (update / source).glob("*.kt"):
            shutil.copy2(file, destination / file.name)
