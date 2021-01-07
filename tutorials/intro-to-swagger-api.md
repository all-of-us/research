# API's and Swagger / OpenAPI

In this module we will modify some of our swagger API signatures, bring up the API server and work with Swagger parameters and add a new API endpoint

## Resources
1. [What is Swagger/OpenAPI](https://swagger.io/docs/specification/2-0/what-is-swagger/)
2. [Swagger/OpenAPI Homepage](https://swagger.io/)
## Overview
Our API is public, it is always theoretically possible for a user to bypass any UI controls we set up by hitting the endpoints directly or via tools.  The UI has restrictions as well, but ultimately its purpose is as a more friendly interface to the API rather than a security mechanism in itself.

For more details on our API endpoints see the [README](https://github.com/all-of-us/workbench) and the [API Code Structure](https://github.com/all-of-us/workbench/blob/master/api/docs/code-structure.md).

Note that we often refer to the AoU RW's back end GAE application as "the api" but we also use "the api" to refer to the collection of endpoints we expose. It can get tricky!

Endpoints are defined in the Swagger/OpenAPI YAML format.  We run code generation tools as part of the standard compilation and deployment processes to create client code in Java and Typescript (for the UI) as well as server interface code in Java for us to implement with the app.

## Caveats / Troubleshooting

1. The build directory may have some empty directories that are needed for the build to succeed
   1. The `/build/classes/kotlin/generated` directory may not exist - you may gave to run `./gradlew compileJava` a few times to get this to work correctly.
   2. This tends to occur after issuing a `clean` commande
2. You may need to restart the local server for your changes to take effect
3. It can be difficult to detect syntax errors in your YAML file. Use a [validator](https://editor.swagger.io/) to ensure that you do not have errors

## Tasks

### Compile The API 

1. In the API directory run: 
    `./gradlew clean compileJava`
    This will generate this error (I am not sure why):
    ` Directory '/home/psantos/projects/broad/workbench/api/build/classes/kotlin/generated' specified for property 'compileGeneratedKotlinOutputClasses' does not exist`
    Run `./gradlew compileJava` again. This should run to completion.

    It will run the `compileGeneratedJava` task, creating and compiling the code generated by swagger and it will compile our hand written java code

    Some additional things you can do to explore the code (in the `api/` directory):
    1. Look in the `build`  directory.
       1. Identify where the generated code is
       2. Identify where some of the generated code is being used in the application
    2. Run `clean` and examine the build directory
       1. What happens?
       2. Re-create the generated code

### (STEP-1) Add/Remove A Parameter From An Endpoint 
In the previous task we generated the swagger java code and compile our handwritten code.
Let's explore some of our handwritten code and see how it interacts with the Swagger/OpenAPI code
1. Go to `src/main/java/org/pmiops/workbench/api/WorkspacesController.java`
   1. Let's look at one of the overriden methods in this file - `getWorkspace`
   2. Add a parameter to this method. Try compiling - what do you think will happen when you compile?
   3. Did your IDE respond to the change? (If not check your IDE settings)
2. Undo your changes above, this time remove a parameter from the method.

### (STEP-2) Bring up the server and inspect an unauthenticated endpoint
1. Run `./project.rb dev-up`
   This should bring up the API server allowing you to send requests to the API endpoints
2. Using a tool like [insomnia](https://insomnia.rest/) query the endpoints
   1. To do this with curl : `curl http://localhost:8081/v1/config`
3. An endpoint you can inspect is `http://localhost:8081/v1/config`
### (STEP-3) Bring up the server and inspect an authenticated endpoint
1. Run `./project.rb dev-up`
   This should bring up the API server allowing you to send requests to the API endpoints
2. Using a tool like [insomnia](https://insomnia.rest/) query the endpoints.
   1. You will need to generate a Bearer token to query the endpoint.
   1. You can do this with the gcp CLI command: `gcloud auth print-access-token`
   1. Make sure you are logged in to your fake-research-aou account on the CLI. You can activate this account with `gcloud config activate <username>@fake-research-aou.org`
   1. To do this with curl: `curl http://localhost:8081/v1/workspaces -H "Accept: application/json" -H "Authorization: Bearer $(gcloud auth print-access-token)"`
3. An endpoint you can inspect is `http://localhost:8081/v1/workspaces`
   This will return all workspaces in your account. If you have not created any workspaces yet it is a good idea to log in to the app and create a workspace so the endpoint will return some data.

### (STEP-4) Update the endpoint to accept a new parameter
1. Open the `workbench-api.yaml` and find the endpoint whose `operationId` is `getWorkspace`
   1. Examine the structure in the yaml file. Note the parameters.
2. Check out the super implementation of `getWorkspace` (`api/build/swagger2/org/pmiops/workbench/api/WorkspacesApiDelegate.java`)
   1. This is the generated code from swagger. It generates this code based on the API definitions in the yaml file, in this case it is the `workbench-api.yaml`
3. Back in `workbench-api.yaml`...
   1. Add a [parameter](https://swagger.io/docs/specification/describing-parameters/) to the `get` method of the API. You can take a look at the `patch` method of the `/v1/workspaces/{workspaceNamespace}/{workspaceId}` path for a live example
   2. Make the parameter a string in the body of the request. You can play around more with parameters after.
   3. Name your parameter "test"
4. Open `api/src/main/java/org/pmiops/workbench/api/WorkspacesController.java`
   1. Go to the `getWorkspace method`
   2. You will need to add the parameter to this method.
5. Restart the server (API-SWAG-3) and inspect your endpoint
   1. When calling the endpoint, be sure to add your parameter to the JSON body: `{"test": "Hello"}`
   2. What happens when you omit this required parameter?

### (STEP-5) Add a new endpoint
1. Open the `workbench-api.yaml` and find the beginning of the endpoint definitions.
2. Create a path for your endpoint, for the purposes of this example I will call the endpoint `/v1/tutorial`
3. Add the `get` method to your endpoint
4. Add the `operationId` to your endpoint (I will call this `getThingWorkspace`)- this is the method that will be called
5. Create a `responses` section in your yaml. 
   1. Define the return value for a 200 response. 
   2. Have your endpoint return a string
6. Add the `workspaces` tag to your endpoint - this will put your API interface in the `WorkspacesApiDelegate`
7. In the `WorkspacesController.java` file write an implementation for the `getThingWorkspace` operation
   1. Have this return a simple string, something like "test response"
8. Start / restart your server
9. Using [Insomnia Core](https://insomnia.rest/products/core) or similar, test our your API
   1.  Remember to generate a bearer token
   2.  Do you see your response?

### (STEP-6) Add a new parameter to your endpoint
After completing (STEP-5) we can add a parameter to your endpoint
1. Similar to (STEP-4) add a parameter to your new endpoint
   1. Pass the parameter in the body, and have it be a string
   2. Return the parameter in the response
2. Using [Insomnia Core](https://insomnia.rest/products/core) or similar, test our your API
   1.  Remember to generate a bearer token
   2.  Do you see your parameter in the response?

### Bonus: Add a new parameter type to your endpoint
Looking at the parameters, most are custom defined in the YAML and referred to a `$ref`. Create your own reference parameter and use it in the endpoint you created in (API-SWAG-5)

## Comprehension Questions

1. Where is the generated code located?
2. What command generated the code?
3. What are two ways you can define a parameter in the Swagger/YAML file?
4. What type of files do you need to touch to add or modify an endpoint?
   1. Can you find an example of two such files that are not mentioned in this tutorial?
      1. What part of the application do they focus on?