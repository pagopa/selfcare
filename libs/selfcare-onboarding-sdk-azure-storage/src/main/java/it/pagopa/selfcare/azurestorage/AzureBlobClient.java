package it.pagopa.selfcare.azurestorage;

import com.azure.storage.blob.models.BlobProperties;

import java.io.File;
import java.util.List;

public interface AzureBlobClient {

  File retrieveFile(String filePath);

  byte[] getFile(String filePath);

  String getFileAsText(String filePath);

  File getFileAsPdf(String contractTemplate);

  String uploadFile(String path, String filename, byte[] data);

  String uploadFilePath(String filePath, byte[] data);

  void removeFile(String fileName);

  BlobProperties getProperties(String filePath);

  List<String> getFiles();

  List<String> getFiles(String path);
}
