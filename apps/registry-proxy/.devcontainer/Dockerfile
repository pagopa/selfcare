FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

# Install Dapr CLI as root
RUN wget -q https://raw.githubusercontent.com/dapr/cli/master/install/install.sh -O /tmp/dapr-install.sh \
    && chmod +x /tmp/dapr-install.sh \
    && /tmp/dapr-install.sh \
    && rm /tmp/dapr-install.sh
