# Promote Release

This GitHub Action promotes a release from UAT (User Acceptance Testing) to PROD (Production). It allows for seamless transitioning of a release from one environment to another.

-  Checkout: Checks out the repository code at a specific reference (```releases/${{ vars.CURRENT_PROD_VERSION }}```). This step prepares the codebase for the release promotion process.

- Promote Repository Release: Uses the GitHub CLI (gh) to edit the release in UAT, setting it as the latest release (--latest) and removing the prerelease status (--prerelease=false). This effectively promotes the release to PROD.

- Set Release PROD Variables: Sets a GitHub repository variable (```CURRENT_PROD_VERSION```) to the current UAT version, updating the production environment's version information.

## Usage

``` yaml
- name: name: Promote Release
  uses: pagopa/selfcare-commons/github-actions-template/promote-release@{sha-commit}
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

## Note

Make sure to properly manage your GitHub tokens. The GITHUB_TOKEN secret is used by default in GitHub Actions, but for accessing repository settings or performing certain actions, you might need a personal access token.
Ensure that your repository has necessary permissions to perform the actions outlined in this workflow.