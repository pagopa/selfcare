# Create Release Branch 

This GitHub Action automates the process of creating release branches for a repository, updating version numbers, and creating releases with release notes.

- Get Latest Release Version: Retrieves the latest release version from existing release branches.
- Set Branch Name: Determines the name of the new release branch based on the version bump.
- Create and Push Branch: Creates a new release branch and pushes the newly created branch to the remote repository.
- Create Repository Release: Creates a new release on GitHub with release notes.
- Create Release Variables: Sets variables for the current UAT (User Acceptance Testing) and production versions.

## Usage

``` yaml
- name: Create Release Branch
  uses: pagopa/selfcare-commons/github-actions-template/create-release@{sha-commit}
  with:
    version-bump: 'patch'
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

## Outputs
``new_branch_name`` Name of the newly created branch.