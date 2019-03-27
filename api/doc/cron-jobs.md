# Cron jobs

The Researcher Workbench (RW) system includes a number of [cron](https://en.wikipedia.org/wiki/Cron) jobs to run
maintenance or synchronization tasks which cannot easily be handled in response to user-triggered web requests.

Typical examples of cron-style tasks include:
 * Cluster maintenance / auto-restart
 * Synchronizing user state from external data sources
 * Periodic security / compliance audit calculations  

All cron jobs are configured as App Engine scheduled tasks; see the
[App Engine docs](https://cloud.google.com/appengine/docs/standard/java/config/cron) for more info.

## Notes

* Cron jobs are implemented as normal API handlers / controller methods in the backend.
* By convention, tag the Swagger method with a name that includes "offline". This clearly identifies the resulting
 controller class as offline-only.
  * For example: "offlineUser" tag --> OfflineUserController.java. 
* Each Swagger method must also be tagged with "cron".
  * This causes the CronInterceptor to be applied, ensuring that the method can only be called from AppEngine's cron infrastructure.
* The deadline on a cron job is __10 minutes__.
  * If we end up needing tasks that take longer, we might split the task out into a separate service with different
    scaling parameters. (See [docs](https://cloud.google.com/appengine/docs/standard/java/config/cronref#deadlines)).
* __Warning__: Cron jobs are executing in an environment where the App Engine service account is the authenticated user,
  __not__ an end-user. Care must be taken to ensure the cron job is always using the proper service-account credentials,
  and/or creating impersonated credentials (e.g. for FireCloud API calls).

## Adding a new cron job

It's straightforward to copy existing patterns when creating a new cron job. Key file locations:

- [workbench.yaml](https://github.com/all-of-us/workbench/blob/master/api/src/main/resources/workbench.yaml) (API definition; search for "cron")
- [OfflineClusterController.java](https://github.com/all-of-us/workbench/blob/master/api/src/main/java/org/pmiops/workbench/api/OfflineClusterController.java) (controller)
- [cron.xml](https://github.com/all-of-us/workbench/blob/master/api/src/main/webapp/WEB-INF/cron.xml) (App Engine cron config)

## Local testing

You can test a cron endpoint locally with curl:

```
curl --header "X-AppEngine-Cron: true" localhost:8081/v1/cron/updateWidgetConfiguration
```

## Viewing logs

One a new cron endpoint has been deployed to test, you should be able to see it in the App Engine "Cron jobs" tab (see
[this link](https://console.cloud.google.com/appengine/cronjobs?project=all-of-us-workbench-test) for the test
environment). Recent success / failure will be highlighted, with links out to Stackdriver logs.
