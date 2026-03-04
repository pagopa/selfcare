# Onboarding SDK Azure Storage

This library has been developed to provide a set of Java utility classes to simplify the work of interact with azure storage. It includes features that helping for upload or download file from an azure storage container.

## Installation

To use this library in your projects, you can add the dependency to your pom.xml if you're using Maven:

```shell script
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>onboarding-sdk-azure-storage</artifactId>
    <version>0.0.1</version>
</dependency>
```

If you are using Gradle, you can add the dependency to your build.gradle file:

```shell script
dependencies {
    implementation 'it.pagopa.selfcare:onboarding-sdk-azure-storage:0.0.1'
}
```

## Usage

Here's an example of how to use AzureBlobClient:

```java script

public class Main {
    public static void main(String[] args) {
        final String azureStorageConnectionString = ... ; // set the azure storage connectionString, for ex.  AccountName=asd;AccountKey=asd;DefaultEndpointsProtocol=http;BlobEndpoint=http://127.0.0.1:10000/account;
        final String azureStorageContainerName = ... ; // set the azure storage container name
        final AzureBlobClient azureBlobClient = new AzureBlobClientDefault(azureStorageConnectionString, azureStorageContainerName);

        //Getting file as text
        final String filepath = ... ;
        final String jsonString = azureBlobClient.getFileAsText(filepath);
    }
}
```