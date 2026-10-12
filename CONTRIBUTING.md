# Contribution Guidelines

## 1. Branching Strategy

The `main` branch contains the integrated project.

Each member must work on a separate feature branch.

Examples:

* `feature/redis-service`
* `feature/virtual-thread-executor`
* `feature/agent-registration`
* `feature/docker-ci`

## 2. Commit Messages

Use descriptive commit messages in this format:

`type: short description`

Examples:

* `feat: implement Redis service`
* `fix: handle Redis connection errors`
* `test: add Redis service tests`
* `docs: update architecture documentation`
* `chore: configure Java 21`

## 3. Signed Commits

Follow the team's agreed commit-signing requirements.

Each member must configure commit signing and verify that GitHub recognizes their signatures.

## 4. Pull Requests

Submit changes through pull requests targeting `main`.

Each pull request should:

* Describe the changes.
* Explain how the changes were tested.
* Contain only relevant changes.
* Pass the required automated checks.
* Receive the required review before merging.

## 5. Code Quality

Write readable, maintainable code and include appropriate tests.

Never commit passwords, API keys, private keys, or other sensitive information.

## 6. Integration

Synchronize feature branches with the latest agreed project baseline.

Resolve conflicts and run appropriate tests before merging.

Coordinate changes to shared APIs, Maven configuration, and application architecture with the relevant module owners.
