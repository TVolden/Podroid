#!/usr/bin/env python3
"""
Cyclomatic Complexity + JaCoCo coverage report for Podroid.

Usage
-----
  # Run tests and generate JaCoCo XML first:
  #   ./gradlew testDebugUnitTest jacocoTestReport
  #
  # Then run this script from the project root:
  #   python3 scripts/cc_and_coverage.py

What it does
------------
1. Walks every .kt file under app/src/main and computes per-method
   Cyclomatic Complexity (CC = decision-point count + 1).
   Decision points counted: if, when-branch, for, while, catch, &&, ||, ?:

2. Parses the JaCoCo XML report and prints per-package instruction /
   branch / method coverage.

3. Prints a global summary.

Limitations
-----------
- The CC counter is a line-based heuristic, not a full AST parse.
  Lambda-heavy code (Compose, Flow pipelines) may read slightly high
  because brace counting is not scope-aware.  Treat results as an
  approximation; values >= 7 should be manually verified.
- JaCoCo XML is only present after `./gradlew jacocoTestReport` has run.
"""

import os
import re
import sys
import xml.etree.ElementTree as ET

# ── configuration ────────────────────────────────────────────────────────────

ROOT        = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN_SRC    = os.path.join(ROOT, "app", "src", "main", "java")
JACOCO_XML  = os.path.join(
    ROOT, "app", "build", "reports", "jacoco",
    "jacocoTestReport", "jacocoTestReport.xml"
)

# CC thresholds
CC_MEDIUM   = 5
CC_HIGH     = 7

# JaCoCo-generated / Hilt / Room classes to exclude from coverage table
GENERATED_PATTERNS = re.compile(
    r'(_Impl|_Factory|_MembersInjector|Hilt_|Module_|DaggerApp'
    r'|\$\$inlined|\$special\$\$|ComposableSingletons|lambda\$)'
)

# ── cyclomatic complexity ─────────────────────────────────────────────────────

DECISION_RE = re.compile(r'\b(if|when|for|while|catch)\b|&&|\|\||\?:')


def compute_cc(src_root: str) -> list[tuple[str, str, str, int]]:
    """
    Returns list of (package_rel, class_name, method_name, cc).
    """
    results = []
    for dirpath, _, filenames in os.walk(src_root):
        for fname in filenames:
            if not fname.endswith(".kt"):
                continue
            fpath = os.path.join(dirpath, fname)
            rel_pkg = os.path.relpath(dirpath, src_root).replace(os.sep, ".")
            class_name = fname[:-3]
            try:
                with open(fpath, encoding="utf-8") as f:
                    lines = f.readlines()
            except OSError:
                continue

            in_fun = False
            fun_name = ""
            depth = 0
            fun_lines: list[str] = []

            for line in lines:
                if not in_fun:
                    m = re.search(r'\bfun\s+(`[^`]+`|\w+)', line)
                    if m:
                        in_fun = True
                        fun_name = m.group(1).strip("`")
                        depth = line.count("{") - line.count("}")
                        fun_lines = [line]
                        if depth < 0:
                            # single-expression fun on one line
                            body = "".join(fun_lines)
                            cc = len(DECISION_RE.findall(body)) + 1
                            results.append((rel_pkg, class_name, fun_name, cc))
                            in_fun = False
                else:
                    fun_lines.append(line)
                    depth += line.count("{") - line.count("}")
                    if depth <= 0:
                        body = "".join(fun_lines)
                        cc = len(DECISION_RE.findall(body)) + 1
                        results.append((rel_pkg, class_name, fun_name, cc))
                        in_fun = False
                        fun_lines = []
                        depth = 0

    return results


def print_cc_report(results: list[tuple[str, str, str, int]]):
    # Sort: highest CC first, then alphabetically
    results.sort(key=lambda r: (-r[3], r[0], r[1], r[2]))

    print("=" * 100)
    print("CYCLOMATIC COMPLEXITY")
    print("=" * 100)
    print(f"{'Package':<40} {'Class':<30} {'Method':<30} {'CC':>4}  Risk")
    print("-" * 100)

    for pkg, cls, meth, cc in results:
        risk = "HIGH  " if cc >= CC_HIGH else ("MEDIUM" if cc >= CC_MEDIUM else "low   ")
        # Only print methods with cc >= 2 to keep output readable
        if cc >= 2:
            print(f"{pkg:<40} {cls:<30} {meth:<30} {cc:>4}  {risk}")

    high   = [r for r in results if r[3] >= CC_HIGH]
    medium = [r for r in results if CC_MEDIUM <= r[3] < CC_HIGH]
    print("-" * 100)
    print(f"Methods with CC >= {CC_HIGH} (HIGH):   {len(high)}")
    print(f"Methods with CC >= {CC_MEDIUM} (MEDIUM): {len(medium)}")
    print(f"Total methods analysed:          {len(results)}")


# ── jacoco coverage ───────────────────────────────────────────────────────────

def pct(missed: int, covered: int) -> str:
    total = missed + covered
    if total == 0:
        return "   -"
    return f"{100 * covered / total:5.1f}%"


def parse_counter(node, ctype: str) -> tuple[int, int]:
    for c in node.findall("counter"):
        if c.attrib["type"] == ctype:
            return int(c.attrib["missed"]), int(c.attrib["covered"])
    return 0, 0


def print_coverage_report(xml_path: str):
    if not os.path.exists(xml_path):
        print(f"\n[coverage] JaCoCo XML not found at {xml_path}")
        print("  Run:  ./gradlew testDebugUnitTest jacocoTestReport")
        return

    tree = ET.parse(xml_path)
    root = tree.getroot()

    print("\n" + "=" * 100)
    print("JACOCO COVERAGE  (unit tests only - Android-dependent code excluded by design)")
    print("=" * 100)
    print(f"{'Package':<55} {'Instr%':>7}  {'Branch%':>8}  {'Method%':>8}  {'Class%':>7}")
    print("-" * 100)

    for pkg in sorted(root.findall("package"), key=lambda x: x.attrib["name"]):
        name = pkg.attrib["name"].replace("dk/lashout/podroid/", "")
        mi, ci = parse_counter(pkg, "INSTRUCTION")
        mb, cb = parse_counter(pkg, "BRANCH")
        mm, cm = parse_counter(pkg, "METHOD")
        mc, cc = parse_counter(pkg, "CLASS")
        print(
            f"{name:<55} {pct(mi,ci):>7}  {pct(mb,cb):>8}  {pct(mm,cm):>8}  {pct(mc,cc):>7}"
        )

    print("-" * 100)
    mi, ci = parse_counter(root, "INSTRUCTION")
    mb, cb = parse_counter(root, "BRANCH")
    mm, cm = parse_counter(root, "METHOD")
    mc, cc = parse_counter(root, "CLASS")
    ml, cl = parse_counter(root, "LINE")
    print(f"{'TOTAL':<55} {pct(mi,ci):>7}  {pct(mb,cb):>8}  {pct(mm,cm):>8}  {pct(mc,cc):>7}")
    print(f"\n  Lines: {cl}/{ml+cl} covered ({pct(ml,cl).strip()})")
    print()


# ── entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    cc_results = compute_cc(MAIN_SRC)
    print_cc_report(cc_results)
    print_coverage_report(JACOCO_XML)
