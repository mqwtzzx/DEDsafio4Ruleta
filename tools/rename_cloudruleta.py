#!/usr/bin/env python3
"""Move the bundled ruleta port to a separate Fabric mod id and namespaces."""
import json
import re
import shutil
from pathlib import Path

root = Path(__file__).resolve().parent.parent
sources = [root / "src/main/java", root / "src/client/java",
           root / "src/main/resources", root / "src/client/resources"]
names = [("ruleta_puerta", "cloudruleta_puerta"),
         ("nuclearcreeper", "cloudruleta_nuclearcreeper"),
         ("supernova", "cloudruleta_supernova"),
         ("ruleta", "cloudruleta")]
extensions = {".java", ".json", ".mcmeta", ".lang", ".properties", ".fsh",
              ".vsh", ".glsl", ".vert", ".frag", ".txt", ".obj", ".mtl",
              ".jem", ".jpm", ".snbt", ".mcfunction", ".svg", ".toml", ".yaml", ".yml"}

def rename(s):
    for old, new in names:
        s = re.sub(r"(?<!cloud)" + re.escape(old), new, s)
    return s

def main():
    metadata = root / "src/main/resources/fabric.mod.json"
    manifest = json.loads(metadata.read_text(encoding="utf-8"))
    if manifest["id"] == "cloudruleta":
        print("Already migrated")
        return
    assert manifest["id"] == "ruleta", manifest["id"]
    entries = [p for folder in sources for p in folder.rglob("*") if p.is_file()]
    staging = root / ".cloudruleta_staging"
    if staging.exists():
        raise RuntimeError("Staging directory exists")
    staging.mkdir()
    try:
        for original in entries:
            relative = original.relative_to(root).as_posix()
            target = staging / rename(relative)
            target.parent.mkdir(parents=True, exist_ok=True)
            data = original.read_bytes()
            if original.suffix.lower() in extensions:
                try:
                    content = data.decode("utf-8")
                except UnicodeDecodeError:
                    pass
                else:
                    content = rename(content)
                    if original == metadata:
                        data_obj = json.loads(content)
                        data_obj["name"] = "CloudRuleta - DEDsafio 4 Fan Project"
                        data_obj["description"] = "DEDsafio 4 Fan Project for Fabric 1.21.1, isolated under the cloudruleta mod id."
                        content = json.dumps(data_obj, ensure_ascii=False, indent=2) + "\n"
                    data = content.encode("utf-8")
            target.write_bytes(data)
        for original in entries:
            original.unlink()
        for staged in staging.rglob("*"):
            if staged.is_file():
                dest = root / staged.relative_to(staging)
                dest.parent.mkdir(parents=True, exist_ok=True)
                staged.replace(dest)
    finally:
        shutil.rmtree(staging, ignore_errors=True)
    build = root / "build.gradle"
    build.write_text(rename(build.read_text(encoding="utf-8")), encoding="utf-8")
    props = root / "gradle.properties"
    text = rename(props.read_text(encoding="utf-8"))
    text = text.replace("mod_version=1.1.0", "mod_version=1.1.0-cloud.1")
    props.write_text(text, encoding="utf-8")
    meta = json.loads(metadata.read_text(encoding="utf-8"))
    assert meta["id"] == "cloudruleta"
    assert meta["entrypoints"]["main"] == ["net.cloudruleta.Ruleta"]
    assert meta["entrypoints"]["client"] == ["net.cloudruleta.client.RuletaClient"]
    for mixin in meta["mixins"]:
        name = mixin if isinstance(mixin, str) else mixin["config"]
        assert (root / "src/main/resources" / name).is_file() or (root / "src/client/resources" / name).is_file(), name
    for folder in sources:
        for p in folder.rglob("*.json"):
            json.loads(p.read_text(encoding="utf-8"))
    print("MIGRATION SUCCESS:", len(entries), "files moved or copied, mod ID cloudruleta")
    print("Resource namespaces cloudruleta, cloudruleta_supernova, cloudruleta_nuclearcreeper, cloudruleta_puerta")

if __name__ == "__main__":
    main()
