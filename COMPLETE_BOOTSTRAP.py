#\!/usr/bin/env python3
# This script creates ALL files and directories for prjeto-post-it
import os
from pathlib import Path

ROOT = Path('/home/mq/iGitHub/prjeto-post-it')
os.chdir(ROOT)

# Step 1: Create directories
print('Step 1: Creating directories...')
directories = [
    'backend/src/main/java/com/postit/domain',
    'backend/src/main/java/com/postit/application/ports',
    'backend/src/main/java/com/postit/application/usecases',
    'backend/src/main/java/com/postit/infrastructure/adapters/in',
    'backend/src/main/java/com/postit/infrastructure/adapters/out',
    'backend/src/main/java/com/postit/infrastructure/config',
    'backend/src/main/java/com/postit/shared/exception',
    'backend/src/main/java/com/postit/shared/error',
    'backend/src/main/resources/db/migration',
    'backend/src/test/java/com/postit/domain',
    'backend/src/test/java/com/postit/application/usecases',
    'backend/src/test/java/com/postit/infrastructure/adapters',
    'backend/src/test/java/com/postit/infrastructure/containers',
    'frontend/src/components',
    'frontend/src/services',
    'frontend/src/utils',
    'frontend/src/assets',
    'frontend/public',
]

for d in directories:
    (ROOT / d).mkdir(parents=True, exist_ok=True)
    print(f'  ✓ {d}')

print('\nStep 2: Creating files...')
