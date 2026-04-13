# AGENTS.md

## Project Overview

This repository is a small workspace for the AgroGem mobile product. The actual application code lives in `app/`, which is a Kotlin Multiplatform project using Compose Multiplatform.

- Root purpose: workspace entry point and shared repository documentation.
- Active product module: `app/`
- Main platforms: Android, iOS, and a WebAssembly preview target.

When working inside `app/` or any of its children, the closer `app/AGENTS.md` file takes precedence.

## Repository Layout

- `README.md` — human-facing overview for the repository
- `app/` — Kotlin Multiplatform application workspace
- `.agents/skills/` — local agent skills used by this repository
- `.atl/skill-registry.md` — skill registry metadata

## Working Rules For Agents

- Treat this repo as a root wrapper around the `app/` project.
- Do not invent root-level workflows that are not present in the repository.
- Prefer updating docs in both places when changing project structure:
  - root `README.md` for repository-level navigation
  - `app/README.md` for implementation and development details
- Avoid touching generated or local-only directories such as `.gradle/`, `.kotlin/`, `build/`, or IDE metadata unless explicitly requested.

## Common Navigation

- Read repository overview: `README.md`
- Work on the app itself: `app/`
- App-specific instructions: `app/AGENTS.md`

## Documentation Expectations

When you change workflows, architecture, or developer commands in `app/`, update:

1. `app/AGENTS.md` for agent-operational guidance
2. `app/README.md` for human readers
3. Root `README.md` if repository navigation or scope changed

## Validation Strategy

There are no root-level build tools configured.

- Validate work at the module level inside `app/`
- Prefer targeted verification over broad commands
- Do not run full build tasks unless explicitly required

## Commit and PR Guidance

- Use conventional commit messages.
- Keep changes scoped to the module you touched.
- Mention whether changes affect repository docs, app docs, or app code.
