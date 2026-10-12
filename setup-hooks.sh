#!/bin/sh

set -e

git config core.hooksPath .githooks

echo "Git hooks configured successfully."
echo "Commit messages will now be validated."