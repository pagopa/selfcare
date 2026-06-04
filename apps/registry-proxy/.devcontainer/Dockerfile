FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye@sha256:4baccbb5d66af8af9a3719c785bc966ec476de5fcf0f603e893cef6a69f59b04

# Install Dapr CLI as root
RUN wget -q https://raw.githubusercontent.com/dapr/cli/master/install/install.sh -O /tmp/dapr-install.sh \
    && chmod +x /tmp/dapr-install.sh \
    && /tmp/dapr-install.sh \
    && rm /tmp/dapr-install.sh
