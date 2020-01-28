# `customs-notifications-receiver-stub`

This service provides: 

- An `POST` endpoint for receiving notification. These are stored in the order they were sent, keyed by 
  Client Subscription ID (`CsId`) which is a UID 
- An `GET` endpoint for retrieving all notifications for a `CsId`
- An `DELETE` endpoint for clearing all stored notifications.

Note this service assumes that we override the use of the `Authorization` header to contain the `CsId` (see below for details).         
Instructions for seeding this data can be found below.
         
# `POST` endpoint for receiving notification

    curl -X POST \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications \
      -H 'authorization: Basic aaaa01f9-ec3b-4ede-b263-61b626dde232' \
      -H 'content-type: application/xml' \
      -H 'x-conversation-id: xxxx01f9-ec3b-4ede-b263-61b626dde232' \
      -d '<foo>bar</foo>'
      
## HTTP return codes

| HTTP Status   | Code Error scenario                                                                              |
| ------------- | ------------------------------------------------------------------------------------------------ |
| 204           | If the request is processed successful.                                                          |
| 400           | This status code will be returned in case of incorrect data,incorrect data format, missing parameters etc. are provided in the request. |
| 415           | If request has missing or invalid Content-Type header.                                             |
| 500           | In case of a system error such as time out, server down etc. ,this HTTP status code will be returned.|

## Request Structure

### HTTP headers

Note we override the use of the `Authorization` header to contain the `CsId` (see below table for details)

| Header              | Mandatory/Optional | Description                                                                 |
| -------------       | -------------------|---------------------------------------------------------------------------- |
| `Content-Type`      | M                  |Fixed `application/xml` or `application/xml; charset=UTF-8`                                       |
| `Authorization`     | M                  |`cccc01f9-ec3b-4ede-b263-61b626dde232` Note this contains the `CsId` which is a UUID  |
| `X-Conversation-ID` | M                  |This id was passed to Messaging when the declaration was passed onto Messaging earlier. This must be a UUID|

### Body
The body of the request will contain the XML notification payload.       
    
## Response Body
      
The Response body will contain the payload that was saved eg:

```json
{
    "csid": "cccc01f9-ec3b-4ede-b263-61b626dde232",
    "conversationId": "xxxx01f9-ec3b-4ede-b263-61b626dde232",
    "authHeaderToken": "Basic cccc01f9-ec3b-4ede-b263-61b626dde232",
    "outboundCallHeaders": [
        {
            "name": "Accept",
            "value": "application/xml"
        },
        {
            "name": "Content-Type",
            "value": "application/xml"
        },
        {
            "name": "Authorization",
            "value": "Basic ffff01f9-ec3b-4ede-b263-61b626dde232"
        },
        {
            "name": "Content-Length",
            "value": "14"
        },
        {
            "name": "X-Conversation-ID",
            "value": "ffff01f9-ec3b-4ede-b263-61b626dde232"
        }
    ],
    "xmlPayload": "<foo>bar</foo>"
}     
```
      
# `GET` endpoints for retrieving all notifications for a `CsId` and ConversationId 

## Request

    curl -X GET \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications/csid/ffff01f9-ec3b-4ede-b263-61b626dde232 \
      -H 'accept: application/json' \
      -H 'content-type: application/json' \

    curl -X GET \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications/conversationid/ffff01f9-ec3b-4ede-b263-61b626dde232 \
      -H 'accept: application/json' \
      -H 'content-type: application/json' \
      
## HTTP return codes

| HTTP Status   | Code Error scenario                                                                              |
| ------------- | ------------------------------------------------------------------------------------------------ |
| 200           | If the request is processed successful.                                                          |
| 400           | This status code will be returned in case of incorrect data,incorrect data format, missing parameters etc. are provided in the request. |
| 500           | In case of a system error such as time out, server down etc. ,this HTTP status code will be returned.|
      
## Response Body     

```json
[
    {
        "csid": "cccc01f9-ec3b-4ede-b263-61b626dde232",
        "conversationId": "xxxx01f9-ec3b-4ede-b263-61b626dde232",
        "authHeaderToken": "Basic cccc01f9-ec3b-4ede-b263-61b626dde232",
        "outboundCallHeaders": [
            {
                "name": "Accept",
                "value": "application/xml"
            },
            {
                "name": "Content-Type",
                "value": "application/xml"
            },
            {
                "name": "Authorization",
                "value": "Basic ffff01f9-ec3b-4ede-b263-61b626dde232"
            },
            {
                "name": "Content-Length",
                "value": "14"
            },
            {
                "name": "X-Conversation-ID",
                "value": "ffff01f9-ec3b-4ede-b263-61b626dde232"
            }
        ],
        "xmlPayload": "<foo>bar</foo>"
    }
]
```

# `GET` endpoint for getting counts of notifications for a `CsId`  

## Request

    curl -X GET \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications/count/csid/ffff01f9-ec3b-4ede-b263-61b626dde232 \
      -H 'accept: application/json' \
      -H 'content-type: application/json' \

# `GET` endpoint for getting counts of notifications for a ConversationId  

## Request

    curl -X GET \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications/count/conversationid/ffff01f9-ec3b-4ede-b263-61b626dde232 \
      -H 'accept: application/json' \
      -H 'content-type: application/json' \
      
# `GET` endpoint for getting counts of all notifications  

## Request

    curl -X GET \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications/totalcount \
      -H 'accept: application/json' \
      -H 'content-type: application/json' \      

# `DELETE` endpoint for clearing all stored notifications.

## HTTP return codes

| HTTP Status   | Code Error scenario                                                                              |
| ------------- | ------------------------------------------------------------------------------------------------ |
| 200           | If the request is processed successful.                                                          |
| 400           | This status code will be returned in case of incorrect data,incorrect data format, missing parameters etc. are provided in the request. |
| 500           | In case of a system error such as time out, server down etc. ,this HTTP status code will be returned.|

## Request

    curl -X DELETE http://localhost:9826/customs-notifications-receiver-stub/pushnotifications
    
## Response

204 with no body

# `POST` endpoint for returning custom HTTP response codes

    curl -X POST \
      http://localhost:9826/customs-notifications-receiver-stub/pushnotifications/customresponse/503 \
      -H 'authorization: Basic aaaa01f9-ec3b-4ede-b263-61b626dde232' \
      -H 'content-type: application/xml' \
      -H 'x-conversation-id: xxxx01f9-ec3b-4ede-b263-61b626dde232' \
      -d '<foo>bar</foo>'

## Request Structure

### HTTP headers

Note we override the use of the `Authorization` header to contain the `CsId` (see below table for details)

| Header              | Mandatory/Optional | Description                                                                 |
| -------------       | -------------------|---------------------------------------------------------------------------- |
| `Content-Type`      | M                  |Fixed `application/xml` or `application/xml; charset=UTF-8`                                       |
| `Authorization`     | M                  |`cccc01f9-ec3b-4ede-b263-61b626dde232` Note this contains the `CsId` which is a UUID  |
| `X-Conversation-ID` | M                  |This id was passed to Messaging when the declaration was passed onto Messaging earlier. This must be a UUID|

### Body
The body of the request can contain anything as it is not parsed     
    
## Response Body
      
The Response body will contain a xml 'errorResponse' element similar to below:

```xml
<errorResponse>
      <code>REQUESTED_ERROR</code>
      <message>Returning HTTP status 503 as requested</message>
</errorResponse>    
```

# Seeding `api-subscription-fields` Declarant data
Note that you will have to seed Declarant URL and SecurityToken data. This can be done by using a curl statement similar to 
the one below:
  
    curl -v -X PUT "http://localhost:9650/field/application/<YOUR_CLIENT_ID_HERE>/context/<YOUR_APP_CONTEXT_HERE>/version/<YOUR_VERSION_HERE>" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications", "securityToken" : "securityToken" } }'

This will generate a MongoDb record in the `notifications` collection in the `api-subscription-fields` database, eg:

```json
{
    "_id" : ObjectId("5b8fab9b9bb4d54201571201"),
    "apiContext" : "customs/inventory-linking-imports",
    "apiVersion" : "1.0",
    "clientId" : "6372609a-f550-11e7-8c3f-9a214cf093aa",
    "fields" : {
        "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications",
        "securityToken" : "securityToken"
    },
    "fieldsId" : "f369eb7e-e6bf-42c6-9902-3c70705684e8"
}
```

Note you will then have to update the `securityToken` field in the database with `fieldsId` value (this is the `CsId`).
This is because the `customs-notifications-receiver-stub` reads the authorisation header in order to extract the `CsId`. So after 
doing this the above record will look like:

```json
{
    "_id" : ObjectId("5b8fab9b9bb4d54201571201"),
    "apiContext" : "customs/inventory-linking-imports",
    "apiVersion" : "1.0",
    "clientId" : "6372609a-f550-11e7-8c3f-9a214cf093aa",
    "fields" : {
        "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications",
        "securityToken" : "f369eb7e-e6bf-42c6-9902-3c70705684e8"
    },
    "fieldsId" : "f369eb7e-e6bf-42c6-9902-3c70705684e8"
}
```

For concrete examples for all Customs services please see below section.

## Concrete Examples of Seeding `api-subscription-fields` Declarant data for Customs services
  
Note you will have to pay attention to the version of the API you are calling - not all versions are covered below.  

| Service                             | CURL for `api-subscription-fields` data seeding                                                  |
| ----------------------------------- | ------------------------------------------------------------------------------------------------ |
| `customs-declarations`              | ```curl -v -X PUT "http://localhost:9650/field/application/6372609a-f550-11e7-8c3f-9a214cf093ad/context/customs%2Fdeclarations/version/2.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications", "securityToken" : "securityToken" } }'``` |
| `customs-inventory-linking-exports` | ```curl -v -X PUT "http://localhost:9650/field/application/6372609a-f550-11e7-8c3f-9a214cf093ae/context/customs%2Finventory-linking%2Fexports/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications", "securityToken" : "securityToken" } }'``` |
| `customs-inventory-linking-imports` | ```curl -v -X PUT "http://localhost:9650/field/application/6372609a-f550-11e7-8c3f-9a214cf093aa/context/customs%2Finventory-linking-imports/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications", "securityToken" : "securityToken" } }'``` |
  
Don't forget that you will have to update the `securityToken` field in the database with `fieldsId` value as mentioned in the above section.  


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
