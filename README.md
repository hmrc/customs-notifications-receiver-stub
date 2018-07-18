# customs-notifications-receiver-stub

This service provides: 

- An `POST` endpoint for receiving notification. These are stored in the order they were sent, keyed by 
  Client Subscription ID (`CsdId`) which is a UID 
- An `GET` endpoint for retrieving all notifications for a `CsdId`
- An `DELETE` endpoint for clearing all stored notifications.
         
# `POST` endpoint for receiving notification

    curl -X POST \
      http://localhost:9826/pushnotifications \
      -H 'accept: application/xml' \
      -H 'authorization: Basic aaaa01f9-ec3b-4ede-b263-61b626dde232' \
      -H 'content-type: application/xml' \
      -H 'x-conversation-id: xxxx01f9-ec3b-4ede-b263-61b626dde232' \
      -d '<foo>bar</foo>'
      
## HTTP return codes

| HTTP Status   | Code Error scenario                                                                              |
| ------------- | ------------------------------------------------------------------------------------------------ |
| 204           | If the request is processed successful.                                                          |
| 400           | This status code will be returned in case of incorrect data,incorrect data format, missing parameters etc. are provided in the request. |
| 406           | If request has missing or invalid ACCEPT header.                                                   |
| 415           | If request has missing or invalid Content-Type header.                                             |
| 500           | In case of a system error such as time out, server down etc. ,this HTTP status code will be returned.|

## Request Structure

### HTTP headers

NOte we override the use of the `Authorization` header to contain the `CsId` (see below for details)

| Header              | Mandatory/Optional | Description                                                                 |
| -------------       | -------------------|---------------------------------------------------------------------------- |
| `Content-Type`      | M                  |Fixed `application/xml; charset=UTF-8`                                       |
| `Accept`            | M                  |Fixed `application/xml`                                                      |
| `Authorization`     | M                  |`Basic cccc01f9-ec3b-4ede-b263-61b626dde232` Note the `CsId` is added after the `Basic` prefix |
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
      
# `GET` endpoint for retrieving all notifications for a `CsdId`  

## Request

    curl -X GET \
      http://localhost:9826/pushnotifications/ffff01f9-ec3b-4ede-b263-61b626dde232 \
      -H 'accept: application/json' \
      -H 'cache-control: no-cache' \
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

# `DELETE` endpoint for clearing all stored notifications.

## HTTP return codes

| HTTP Status   | Code Error scenario                                                                              |
| ------------- | ------------------------------------------------------------------------------------------------ |
| 200           | If the request is processed successful.                                                          |
| 400           | This status code will be returned in case of incorrect data,incorrect data format, missing parameters etc. are provided in the request. |
| 500           | In case of a system error such as time out, server down etc. ,this HTTP status code will be returned.|

## Request

    curl -X DELETE http://localhost:9826/pushnotifications
    
## Response

204 with no body
    
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
