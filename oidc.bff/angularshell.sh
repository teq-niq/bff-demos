#!/bin/bash

# Absolute path to project (project root)
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Force angular-front-end as working directory
cd "$PROJECT_DIR/angular-front-end" || exit

# Isolated PATH (does NOT include existing PATH)
export PATH="$PROJECT_DIR/../node:$PROJECT_DIR/angular-front-end/node_modules/.bin:$VS_CODE_HOME/bin"

echo "Using isolated Node environment:"
node -v
npm -v
ng version

# Start shell in angular-front-end
exec "$SHELL"