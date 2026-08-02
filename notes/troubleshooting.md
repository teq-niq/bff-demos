# Troubleshooting

Troubleshooting tips collected as issues are encountered and resolved.

## npm fails with `Cannot find module '../../package.json'`

### Symptom

A build using the `npmbuild` profile reports that Node.js is already installed, but the subsequent npm step fails:

```text
[INFO] Node v24.0.0 is already installed.
[INFO] --- frontend:1.15.4:npm (npm install - angular-front-end) @ simple.bff ---
[INFO] Error: Cannot find module '../../package.json'
[INFO] Require stack:
[INFO] - ...\bff-demos\node\node_modules\npm\lib\cli\validate-engines.js
[ERROR] Failed to execute goal com.github.eirslett:frontend-maven-plugin:1.15.4:npm
```

### Cause

The local `node` installation may be incomplete. For example, interrupting the first build with `Ctrl+C` can leave `node.exe` installed while npm is only partially extracted. On the next build, `frontend-maven-plugin` detects `node.exe` and skips reinstalling Node.js and npm.

### Resolution

From the `bff-demos` root, delete the generated `node` directory and rerun the build. The directory is excluded from Git and will be recreated automatically.

Command Prompt:

```cmd
rmdir /s /q node
mvn clean package -P npmbuild
```

PowerShell:

```powershell
Remove-Item -Recurse -Force node
mvn clean package -P npmbuild
```
