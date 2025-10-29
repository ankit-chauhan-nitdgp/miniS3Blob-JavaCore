# miniS3Blob-JavaCore
trying to replicate s3 basic features using java core libraries

## Core endpoints
### PUT 
http://localhost:9000/bucket/docs/test1.jpg

### PUT SIGNED
http://localhost:9000/bucket/docs/test1.jpg?expires=<TimeMills>&signature=<Hmac-signature>

### GET 
http://localhost:9000/bucket/docs/test1.jpg

### GET SIGNED
http://localhost:9000/bucket/docs/test1.jpg?expires=<TimeMills>&signature=<Hmac-signature>

### DELETE 
http://localhost:9000/bucket/docs/test1.jpg
