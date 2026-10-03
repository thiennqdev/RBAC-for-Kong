const http = require('http');

const createServer = (port) => {
  const server = http.createServer((req, res) => {
    const timestamp = new Date().toISOString();
    
    // Log the request to demonstrate which server is receiving traffic
    // This will help visualize Round Robin (alternating between 8081 and 8082)
    // and Healthchecks (periodic requests to a health endpoint)
    console.log(`[${timestamp}] [Port ${port}] Received ${req.method} request for ${req.url}`);

    // Optional: Simulate a failure for healthcheck testing
    // If you want to test healthchecks failing, you could add logic like:
    // if (req.url === '/health' && port === 8081 && someCondition) {
    //   res.writeHead(500);
    //   res.end('Internal Server Error');
    //   return;
    // }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      message: `Hello from mock server on port ${port}`,
      port: port,
      path: req.url,
      timestamp: timestamp
    }));
  });

  server.listen(port, () => {
    console.log(`Mock server is listening on port ${port}`);
  });
};

// Start servers on 8081 and 8082
createServer(8081);
createServer(8082);
