"""Install the audited changes on top of the development V0.16 materialization."""
import argparse
from pathlib import Path
import shutil
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--server-only", action="store_true")
args = parser.parse_args()
update = Path(__file__).resolve().parent
root = Path.cwd()


def apply_patch(name):
    subprocess.run(["git", "apply", "--check", str(update / name)], cwd=root, check=True)
    subprocess.run(["git", "apply", str(update / name)], cwd=root, check=True)


def copy_files(source, target):
    target.mkdir(parents=True, exist_ok=True)
    for file in sorted(source.iterdir()):
        shutil.copy2(file, target / file.name)


apply_patch("server.patch")
copy_files(update / "server", root / "server")
shutil.copy2(root / "updates/v0_13/catalog.tsv", root / "server/live_catalog.tsv")

if not args.server_only:
    apply_patch("android.patch")
    app = root / "android-app/app/src"
    copy_files(update / "android/main", app / "main/java/com/mlbb/assistant")
    copy_files(update / "android/test", app / "test/java/com/mlbb/assistant")
    (app / "base").mkdir(exist_ok=True)
    shutil.copy2(update / "android/AndroidManifest.xml", app / "base/AndroidManifest.xml")
