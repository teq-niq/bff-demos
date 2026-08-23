#!/bin/bash

# Absolute path to project (project root)
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Force angular-front-end as working directory
cd "$PROJECT_DIR/angular-front-end" || exit

# Isolated toolchain plus the minimal Linux command paths required by Bash.
export PATH="$PROJECT_DIR/../node:$PROJECT_DIR/angular-front-end/node_modules/.bin:/usr/bin:/bin"

echo "Using isolated Maven-managed Node environment:"
node -v
npm -v
ng version

# Start shell in angular-front-end
exec "$SHELL"