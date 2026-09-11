#!/usr/bin/env python3
import os
import re
import subprocess
import sys

GRADLE_FILE = "app/build.gradle.kts"

def get_existing_tags():
    try:
        res = subprocess.run(["git", "tag", "-l"], stdout=subprocess.PIPE, text=True, check=True)
        return set(res.stdout.strip().splitlines())
    except Exception:
        return set()

def main():
    bump_type = sys.argv[1] if len(sys.argv) > 1 else "patch"
    if bump_type not in ("patch", "minor", "major"):
        bump_type = "patch"

    with open(GRADLE_FILE, "r") as f:
        content = f.read()

    vc_match = re.search(r"versionCode\s*=\s*(\d+)", content)
    vn_match = re.search(r"versionName\s*=\s*\"([\d\.]+)\"", content)

    if not vc_match or not vn_match:
        print("ERROR: Could not find versionCode or versionName in build.gradle.kts", file=sys.stderr)
        sys.exit(1)

    current_vc = int(vc_match.group(1))
    current_vn = vn_match.group(1)

    parts = [int(p) for p in current_vn.split(".")]
    while len(parts) < 3:
        parts.append(0)

    major, minor, patch = parts[0], parts[1], parts[2]

    if bump_type == "major":
        major += 1
        minor = 0
        patch = 0
    elif bump_type == "minor":
        minor += 1
        patch = 0
    else: # patch
        patch += 1

    existing_tags = get_existing_tags()
    next_vn = f"{major}.{minor}.{patch}"
    while f"v{next_vn}" in existing_tags:
        patch += 1
        next_vn = f"{major}.{minor}.{patch}"

    next_vc = current_vc + 1

    new_content = re.sub(r"(versionCode\s*=\s*)\d+", rf"\g<1>{next_vc}", content, count=1)
    new_content = re.sub(r"(versionName\s*=\s*)\"[\d\.]+\"", rf'\g<1>"{next_vn}"', new_content, count=1)

    with open(GRADLE_FILE, "w") as f:
        f.write(new_content)

    tag = f"v{next_vn}"
    print(f"Bumped version to {next_vn} (versionCode {next_vc}, tag {tag})")

    # Set GitHub Actions step output if running in workflow
    gh_output = os.environ.get("GITHUB_OUTPUT")
    if gh_output:
        with open(gh_output, "a") as f:
            f.write(f"version={next_vn}\n")
            f.write(f"version_code={next_vc}\n")
            f.write(f"tag={tag}\n")

if __name__ == "__main__":
    main()
