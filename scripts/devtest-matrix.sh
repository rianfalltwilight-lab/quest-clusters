#!/usr/bin/env bash
# Runs the in-game self-test (:core:runClientDevtest) once per mod combination and prints a summary.
#
#   scripts/devtest-matrix.sh              all profiles
#   scripts/devtest-matrix.sh default rei  only these
#   scripts/devtest-matrix.sh --list       show the profiles
#
# Logs:    build/devtest-matrix/<profile>.log
# Summary: build/devtest-matrix/summary.md (also printed)
# Exit:    0 only if every profile reached "ALL CHECKS PASSED". Gradle itself exits 0 even when checks fail,
#          so the result is read from the log; a run without the final line (crash, timeout) counts as failed.
#
# Environment: DEVTEST_TIMEOUT (seconds per profile, default 900), GRADLE_ARGS (extra Gradle arguments).
# Quest Enhance / Certain Questing Additions combinations need jars that are not on a maven: run those by hand.
set -u

cd "$(dirname "$0")/.."

profiles() {
  cat <<'LIST'
default    published modules (visuals, editor, filters, xmod) without optional mods
core-only  core alone (-PskipModules=visuals,editor,filters,xmod)
no-ffs     published modules without FTB Filter System (filters disables itself)
addons     KubeJS + Jade + GuideME + Patchouli, no XMod (stages, Jade views, docs targets)
xmod-jei   FTB XMod Compat + KubeJS + Jade + docs mods + JEI (take-over of XMod's quest part)
rei        REI instead of JEI
LIST
}

flags() {
  case "$1" in
    default)   echo "" ;;
    core-only) echo "-PskipModules=visuals,editor,filters,xmod" ;;
    no-ffs)    echo "-PnoFfs" ;;
    addons)    echo "-PwithKubejs -PwithJade -PwithDocs" ;;
    xmod-jei)  echo "-PwithXmod -PwithKubejs -PwithJade -PwithDocs -PwithJei" ;;
    rei)       echo "-PwithRei" ;;
    *)         return 1 ;;
  esac
}

if [ "${1:-}" = "--list" ]; then
  profiles
  exit 0
fi

selected=("$@")
if [ ${#selected[@]} -eq 0 ]; then
  selected=($(profiles | awk '{print $1}'))
fi
for p in "${selected[@]}"; do
  flags "$p" > /dev/null || { echo "unknown profile: $p (see --list)" >&2; exit 2; }
done

out=build/devtest-matrix
mkdir -p "$out"
limit="${DEVTEST_TIMEOUT:-900}"
timeout_cmd=""
if command -v timeout > /dev/null; then timeout_cmd="timeout $limit"; elif command -v gtimeout > /dev/null; then timeout_cmd="gtimeout $limit"; fi

summary="$out/summary.md"
{
  echo "| Profile | Result | Passed | Failed | Time | Flags |"
  echo "|---|---|---|---|---|---|"
} > "$summary"
failed_any=0

for p in "${selected[@]}"; do
  f="$(flags "$p")"
  log="$out/$p.log"
  echo "==> $p ${f:-(no flags)}"
  start=$(date +%s)
  # shellcheck disable=SC2086
  $timeout_cmd ./gradlew :core:runClientDevtest --console=plain $f ${GRADLE_ARGS:-} > "$log" 2>&1
  code=$?
  secs=$(( $(date +%s) - start ))
  passed=$(grep -c '\[devtest\] PASS ' "$log")
  failed=$(grep -c '\[devtest\] FAIL ' "$log")
  if grep -q '\[devtest\] ALL CHECKS PASSED' "$log"; then
    result="ok"
  elif grep -q '\[devtest\] [0-9]* CHECK(S) FAILED' "$log"; then
    result="FAILED"
  elif [ $code -eq 124 ]; then
    result="TIMEOUT"
  else
    result="ERROR (exit $code, no final line)"
  fi
  [ "$result" = "ok" ] || failed_any=1
  echo "| $p | $result | $passed | $failed | $((secs / 60))m$((secs % 60))s | \`${f:-}\` |" >> "$summary"
  echo "    $result: $passed passed, $failed failed (${secs}s), log: $log"
  grep '\[devtest\] FAIL \|\[devtest\] crashed' "$log" | sed 's/^.*\[devtest\] /    /' | head -20
done

echo
cat "$summary"
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  { echo "### In-game self-test"; echo; cat "$summary"; } >> "$GITHUB_STEP_SUMMARY"
fi
exit $failed_any
