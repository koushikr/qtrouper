### Version 0.0.1-2

- Added default handling to QTrouper.handle in case the properties object goes missing or when the headers are not present.
- Doing a minor, for this is a bug fix. 

## Impact

If you are using trouper to publish messages and read off it, this won't impact you. But when you are publishing messages using another RMQ client or an adhoc script that pushes messages into the queue without the headers, required (that trouper would've organically added), you'll see this issue. 
