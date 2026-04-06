#!/usr/bin/env bash
set -euo pipefail

FEATURE_DIR="specs/002-definir-fundacao-modular"

required_files=(
  "$FEATURE_DIR/spec.md"
  "$FEATURE_DIR/plan.md"
  "$FEATURE_DIR/tasks.md"
  "$FEATURE_DIR/research.md"
  "$FEATURE_DIR/data-model.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file"
    exit 1
  fi
done

if ! grep -q "Requirements Traceability Matrix" "$FEATURE_DIR/plan.md"; then
  echo "plan.md missing traceability matrix"
  exit 1
fi

if ! grep -q "Constitution Validation Checkpoints" "$FEATURE_DIR/tasks.md"; then
  echo "tasks.md missing constitutional checkpoints"
  exit 1
fi

if ! grep -q "T015" "$FEATURE_DIR/tasks.md" || ! grep -q "T022" "$FEATURE_DIR/tasks.md"; then
  echo "tasks.md missing foundational database tasks"
  exit 1
fi

echo "Drift verification passed"
