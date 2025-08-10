import os
import shutil
import subprocess
from sys import stderr

from hatchling.builders.hooks.plugin.interface import BuildHookInterface


class CustomBuildHook(BuildHookInterface):
    def initialize(self, version, build_data):
        super().initialize(version, build_data)

        # 检查是否跳过构建
        if os.environ.get('SKIP_BUILD', '').lower() in ('1', 'true', 'yes'):
            stderr.write(">>> Skipping frontend build (SKIP_BUILD=true)\n")
            return

        stderr.write(">>> Building Open Webui frontend\n")
        npm = shutil.which("npm")
        if npm is None:
            raise RuntimeError(
                "NodeJS `npm` is required for building Open Webui but it was not found"
            )
        stderr.write("### npm install\n")
        subprocess.run([npm, "install", "--force"], check=True)  # noqa: S603
        stderr.write("\n### npm run build\n")
        os.environ["APP_BUILD_HASH"] = version
        subprocess.run([npm, "run", "build"], check=True)  # noqa: S603
