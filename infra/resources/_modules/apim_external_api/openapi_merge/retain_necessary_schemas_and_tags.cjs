const fs = require('fs');

const openApiFilePath = process.argv[2];

// Load the OpenAPI JSON file
let openApiData = JSON.parse(fs.readFileSync(openApiFilePath, 'utf8'));

// Function to get all schema references from the paths
function getSchemaReferences(paths) {
  const schemaRefs = new Set();
  
  function extractRefsFromContent(content) {
    if (content && content.schema && content.schema.type === 'array' && content.schema.items.$ref) {
      schemaRefs.add(content.schema.items.$ref);
    } else if (content && content.schema && content.schema.$ref) {
      schemaRefs.add(content.schema.$ref);
    }
  }
  
  for (const path in paths) {
    for (const method in paths[path]) {
      const responses = paths[path][method].responses;
      for (const response in responses) {
        const content = responses[response].content;
        if (content) {
          for (const mediaType in content) {
            extractRefsFromContent(content[mediaType]);
          }
        }
      }

      const requestBody = paths[path][method].requestBody;
      if (requestBody && requestBody.content) {
        for (const mediaType in requestBody.content) {
          extractRefsFromContent(requestBody.content[mediaType]);
        }
      }

      const parameters = paths[path][method].parameters;
      if (parameters) {
        for (const index in parameters) {
          extractRefsFromContent(parameters[index]);
        }
      }
    }
  }
  
  return Array.from(schemaRefs);
}

// Function to resolve all nested schema references
function resolveNestedReferences(schemaRefs, components) {
  const resolvedRefs = new Set();
  
  function addSchemaAndDependencies(schemaName) {
    if (!resolvedRefs.has(schemaName) && components.schemas[schemaName]) {
      resolvedRefs.add(schemaName);
      const schema = components.schemas[schemaName];

      function extractRefsFromSchema(schema) {
        if (schema.$ref) {
          const refSchemaName = schema.$ref.split('/').pop();
          addSchemaAndDependencies(refSchemaName);
        } else if (schema.type === 'object' && schema.properties) {
          for (const prop in schema.properties) {
            extractRefsFromSchema(schema.properties[prop]);
          }
        } else if (schema.type === 'array' && schema.items) {
          extractRefsFromSchema(schema.items);
        } else if (schema.additionalProperties) {
          extractRefsFromSchema(schema.additionalProperties);
        }
      }

      extractRefsFromSchema(schema);
    }
  }
  
  schemaRefs.forEach(ref => {
    const schemaName = ref.split('/').pop();
    addSchemaAndDependencies(schemaName);
  });
  
  return Array.from(resolvedRefs);
}

// Function to get all tags used in the paths
function getTagsUsedInPaths(paths) {
  const tags = new Set();
  
  for (const path in paths) {
    for (const method in paths[path]) {
      const methodTags = paths[path][method].tags;
      if (methodTags) {
        methodTags.forEach(tag => tags.add(tag));
      }
    }
  }
  
  return Array.from(tags);
}

// Extract initial schema references from paths
const initialSchemaRefs = getSchemaReferences(openApiData.paths);

// Resolve nested schema references
const finalSchemaRefs = resolveNestedReferences(initialSchemaRefs, openApiData.components);

// Filter components.schemas to retain only the required ones
const filteredSchemas = {};
finalSchemaRefs.forEach(ref => {
  filteredSchemas[ref] = openApiData.components.schemas[ref];
});
openApiData.components.schemas = filteredSchemas;

// Extract tags used in paths
const usedTags = getTagsUsedInPaths(openApiData.paths);

// Filter openApiData.tags to retain only the used ones
if (openApiData.tags) {
  openApiData.tags = openApiData.tags.filter(tag => usedTags.includes(tag.name));
}

// Save the filtered OpenAPI data back to the file
fs.writeFileSync(openApiFilePath, JSON.stringify(openApiData, null, 2), 'utf8');

console.log('Filtered openapi.json has been saved.');
