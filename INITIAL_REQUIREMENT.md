Payment processing System

Build a robust backend application that handles payment integration with Authorize.Net Sandbox API. The vision is for all the Developers to work on Payment gateway integration which is a very common task in many domains and utilize AI tools to the fullest to overcome the challenges which come with this task. 
The service must support core payment flows (like purchase, refund, cancel, authorize/capture) and advanced flows (like recurring billing, idempotent retries, async webhook handling).
You will build a service, which will support these features using Authorize.net as payment gateway.

1. Core Functional Requirements
Implement the following flows against Authorize.Net sandbox: 
1.	Purchase (auth + capture in one step). 
2.	Authorize + Capture (two-step). 
3.	Cancel (before capture). 
4.	Refunds (full + partial). 
5.	Subscriptions / Recurring Billing – set up recurring payments (e.g., monthly plan). 
6.	Idempotency & Retries – ensure safe retry of requests (e.g., duplicate webhook events or retrying failed captures). 
7.	Webhooks – implement webhook handlers for async payment events (payment success/failure, refund completion). 
8.	Distributed Tracing – every request/response should include a correlation ID, logs must trace flows end-to-end. 
9.	Scalability considerations – queue-based webhook/event handling (in-memory or message broker). 
10.	Compliance considerations – add a section in docs covering PCI DSS handling, secrets management, rate limits, audit logs.
Expectations: 
•	Expose endpoints for each action, including but not limited to:
Purchase, Cancel, Refund, Subscription / Recurring Billing Management
•	Use JWT authentication for your own service endpoints. 
•	API key–based integration with Authorize.Net (sandbox credentials). 
•	Persist orders & transaction history in DB. 
•	Return clear error responses for invalid requests. 
•	Provide unit tests with coverage report. 


2. Technical Constraints & Rules
•	Must integrate directly with Authorize.Net Sandbox API. 
•	Language/stack of your choice (Java, Python, JS/TS, C#, Go, etc.). 
•	No third-party “all-in-one” wrappers—use official SDK if available for your language. 
•	Must support unit testing (≥80% coverage). 
•	implement distributed tracing (correlation IDs in logs + metrics endpoint)


3. Must contain this.
Kindly note the names of each of the expected files should be the same. The automated evaluation mechanism expects that those file names are accurate, if not then it will impact the final score.

GitHub repository containing the following: 
1.	Source Code: The complete, running source code for the application.  

2.	README.md: A clear overview of the project and detailed instructions on how to set up the database and run the application and its background workers.

3.	PROJECT_STRUCTURE.md: Explaining the structure of the project and the purpose for each of the folder and key modules.

4.	Architecture.md: A simple document or Postman collection defining the APIendpoints you built.
o	Overview of flows implemented. 
o	DB schema & entity relationships. 
o	Design trade-offs (e.g., sync vs async, retry strategies, queueing)
o	Compliance considerations.

5.	OBSERVABILITY.md: metrics list, tracing/logging strategy. 

6.	API-SPECIFICATION.yml: A simple document or Postman collection defining the API endpoints you built.
o	The file name should be POSTMAN_COLLECTION.json in case of a postman collection.
o	The file name should be API-SPECIFICATION.md if it is a markdown file.
o	The file name should be API-SPECIFICATION.yml if it is an API sepecification file.

7.	docker-compose.yml: A single, working Docker Compose file that starts all required components of your system for easy validation.

8.	CHAT_HISTORY.md: A summary document that chronicles your design journey with your AI assistant, highlighting key decision points and how you used AI to evaluate alternatives.

9.	TESTING_STRATEGY.md – Plan and Strategy for preparing Test cases. 
10.	TEST_REPORT.md – unit test coverage summary. 
