"""Unit tests for scripts/check-java (JDK major version preflight)."""
import os
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "check-java"


def fake_jdk(root: Path, name: str, version_line: str) -> Path:
    """Create a fake JAVA_HOME whose bin/java prints the given first line of `java -version`."""
    home = root / name
    (home / "bin").mkdir(parents=True)
    java = home / "bin" / "java"
    java.write_text("#!/usr/bin/env bash\nprintf '%s\\n' " + repr(version_line) + " >&2\n")
    java.chmod(java.stat().st_mode | stat.S_IXUSR)
    return home


def run(java_home, *args, path=None):
    env = {k: v for k, v in os.environ.items() if k != "JAVA_HOME"}
    if java_home is not None:
        env["JAVA_HOME"] = str(java_home)
    if path is not None:
        env["PATH"] = path
    return subprocess.run([str(SCRIPT), *args], env=env, capture_output=True, text=True)


class CheckJavaTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)

    def tearDown(self):
        self._tmp.cleanup()

    def test_accepts_required_major(self):
        home = fake_jdk(self.root, "jdk25", 'openjdk version "25.0.5" 2026-07-21 LTS')
        result = run(home)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("ci: java 25.0.5 em", result.stdout)
        self.assertIn(str(home / "bin" / "java"), result.stdout)

    def test_rejects_older_major_with_portuguese_hint(self):
        home = fake_jdk(self.root, "jdk21", 'openjdk version "21.0.12.1" 2026-08-18')
        result = run(home)
        self.assertEqual(result.returncode, 1)
        self.assertIn("JDK 25 obrigatorio", result.stderr)
        self.assertIn("versao 21.0.12.1", result.stderr)
        self.assertIn("exporte JAVA_HOME apontando para um JDK 25", result.stderr)
        self.assertEqual(result.stdout, "")

    def test_rejects_newer_major(self):
        home = fake_jdk(self.root, "jdk26", 'openjdk version "26" 2027-03-16')
        result = run(home)
        self.assertEqual(result.returncode, 1)
        self.assertIn("versao 26", result.stderr)

    def test_handles_legacy_version_scheme(self):
        home = fake_jdk(self.root, "jdk8", 'openjdk version "1.8.0_292"')
        result = run(home)
        self.assertEqual(result.returncode, 1)
        self.assertIn("versao 1.8.0_292", result.stderr)
        self.assertEqual(run(home, "8").returncode, 0)

    def test_required_major_is_configurable(self):
        home = fake_jdk(self.root, "jdk21", 'openjdk version "21.0.12.1" 2026-08-18')
        self.assertEqual(run(home, "21").returncode, 0)

    def test_java_home_takes_precedence_over_path(self):
        wrong = fake_jdk(self.root, "jdk21", 'openjdk version "21.0.12.1" 2026-08-18')
        right = fake_jdk(self.root, "jdk25", 'openjdk version "25.0.5" 2026-07-21 LTS')
        path = f"{right / 'bin'}{os.pathsep}{os.environ.get('PATH', '')}"
        self.assertEqual(run(wrong, path=path).returncode, 1)
        self.assertEqual(run(None, path=path).returncode, 0)

    def test_missing_java_home_binary(self):
        result = run(self.root / "does-not-exist")
        self.assertEqual(result.returncode, 1)
        self.assertIn("nenhum executavel java encontrado", result.stderr)
        self.assertIn("exporte JAVA_HOME apontando para um JDK 25", result.stderr)

    def test_unparseable_version_output(self):
        home = fake_jdk(self.root, "broken", "not a java binary")
        result = run(home)
        self.assertEqual(result.returncode, 1)
        self.assertIn("versao desconhecida", result.stderr)


if __name__ == "__main__":
    unittest.main()
