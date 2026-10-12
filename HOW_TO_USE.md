# Using these files with GitHub Copilot in VS Code

## 1. Copy into your ledgerflow repo (keep the same paths)
```
docs/SPEC.md                        your full original spec, word for word
.github/copilot-instructions.md     conventions Copilot reads automatically on every chat
.github/prompts/phase-N-*.prompt.md one ready prompt per phase (0 to 9)
```
This file (HOW_TO_USE.md) does not need to go in the repo.

Commit them first:
```
git add docs/SPEC.md .github/copilot-instructions.md .github/prompts
git commit -m "docs: add spec, Copilot instructions and phase prompts"
git push
```
If the Phase 0 skeleton from Claude is already in the repo, these paths don't clash with it.

## 2. Run a phase
1. Open Copilot Chat in VS Code and switch to **Agent** mode (so it can create files and run commands).
2. Type `/` and pick the phase prompt, e.g. `/phase-0-repo-setup`. (Or open the prompt file and click the run button at the top.)
   If your VS Code version doesn't list them, open the file, copy everything below the `---` header, and paste it into chat.
3. Copilot does ONE step, writes its tests, gives you commands, and stops. Run the commands, check tests are green, commit, then type `next`.
4. At the end of each phase, ask for the 3 interview questions if it didn't give them.

## 3. Install the design skills you listed (before Phase 4)
```
npx skills add https://github.com/Leonxlnx/taste-skill --skill "design-taste-frontend"
```
The other links (21st magic MCP, web design guidelines, image-to-code, awesome-design-md, Playwright CLI) are listed at the bottom of `docs/SPEC.md`, and the Phase 4 prompt tells Copilot to use them. Check each project's README for its current install steps.

## Tips
- Keep chats short: start a new chat per phase so old context doesn't crowd the new one. The instructions file and SPEC.md carry everything across.
- If Copilot names a library version, verify it on Maven Central or npm before accepting.
- Bring design questions, reviews and interview prep back to the LedgerFlow project in Claude.
