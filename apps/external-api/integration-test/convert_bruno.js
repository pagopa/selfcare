var fs = require('fs');

fs.readFile('integration-test/integration-test-result.json', 'utf8', function (err, data) {
  if (err) throw err; // we'll not consider error handling for now
  var obj = JSON.parse(data);

  let json = JSON.stringify(convert(obj[0]["summary"], process.argv[2]));
  fs.writeFile("integration-test/stats.json", json, 'utf8', (err) => {
    if (err) {
      console.error('Error writing to file', err);
    } else {
      console.log('Data written to file');
    }
  });
});

function convert(payload) {
  var block =
  {
    "blocks": [
      {
        "type": "section",
        "text": {
          "type": "mrkdwn",
          "text": "*:dog: External API Integration Tests*"
        }
      },
      {
        "type": "section",
        "fields": [
          {
            "type": "mrkdwn",
            "text": `*Requests:*\ntotal: ${payload["totalRequests"]} passed: ${payload["passedRequests"]} failed: ${payload["failedRequests"]}\nskipped: ${payload["skippedRequests"]} error: ${payload["errorRequests"]}`
          }
        ]
      }
    ]
  }

  return block
}
