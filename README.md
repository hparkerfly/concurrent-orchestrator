# Translation Service

This is a reduced version of a real use case which relies on translations done by external APIs from several countries as well as DB information.

When a request consisting of a list of words is received there are two case scenarios:

1. The number `n` of words is above the threshold: `n` threads are created to call the external API and one thread is created to get the data from the DB.
2. The number `n` of words is below the threshold: `n` threads are created to call the external API and `n` threads are created to get the data from the DB.

Common behaviour:

1. A non blocking thread sleep with a parameterized wait is used along the DB future to give some time for the API to respond. This way we won't always have the results from the DB as it is way faster than the API.
2. When the wait finishes either of the results will be returned if it's done. This way we are sure the request won't take too long.
3. The translations from the API are better than the ones from the DB so each thread calling the API will update the DB when it finishes.
4. Any error will be notified to an external system.

Points to bear in mind:
1. The main logic is in the class `OrchestrationService`. It handles and orchestrates everything thread related.
2. The services which retrieve the translations from API/DB have been reduced so that they do `String::upperCase` with some sleeping to simulate waits.
