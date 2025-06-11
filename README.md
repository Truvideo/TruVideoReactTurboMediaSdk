# TruVideoReactTurboMediaSdk

## truvideo-react-turbo-media-sdk

none

## Installation

```sh
npm install https://github.com/Truvideo/TruVideoReactTurboMediaSdk.git
```

## Usage


```js
import { MediaBuilder,UploadProgressEvent,UploadCompleteEventData,UploadErrorEvent } from 'truvideo-react-turbo-media-sdk';

// ...

// init builder
const result = new MediaBuilder(item.filePath)
// setTag
result.setTag("key","value");
result.setTag("color","red");
result.setTag("orderNumber","123");
// setMetaData
result.setMetaData("key","value");
result.setMetaData("key1","1");
result.setMetaData("key2","[4,5,6]");
// buiild request
console.log(' successful: set data');
var request = await result.build()
console.log(' successful: set build');
// handle callbacks
const uploadCallbacks = {
        onProgress: (event: UploadProgressEvent) => {
            console.log(`ID: ${event.id}, Progress: ${event.progress}%`)
        },
        onComplete: (event: UploadCompleteEventData) => { // Use 'any' or proper type for parsed data
            console.log(`ID: ${event.id}, Type: ${event.fileType}`)
        },
        onError: (event: UploadErrorEvent ) => {
            console.log(`ID: ${event.id}, Error: ${event.error}`)
        },
};

//const result = await uploadMedia(item.filePath, JSON.stringify(tag), JSON.stringify(metaData));
var res = await request.upload(uploadCallbacks)
console.log(' successful: set upload');

// pause
await request.pause(uploadCallbacks)

// resume
await request.resume(uploadCallbacks)

// delete
await request.delete(uploadCallbacks)
```


## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
