# TruVideoReactTurboMediaSdk

## truvideo-react-turbo-media-sdk

none

## Installation

```sh
npm install https://github.com/Truvideo/TruVideoReactTurboMediaSdk.git
```

## Usage


```js
import { MediaBuilder } from 'truvideo-react-turbo-media-sdk';

// ...

// init builder
const result = new MediaBuilder("filepath")
// setTag
result.setTag("key","value")
// setMetaData
result.setMetaData("key","value")
// buiild request
var request = await result.build()

// handle callbacks
const uploadCallbacks = {
        onProgress: (event: { id: string; progress: number }) => {

        },
        onComplete: (event: any) => { // Use 'any' or proper type for parsed data

        },
        onError: (event: { id: string; error: any }) => {

        },
  };

// get upload url
var upload = await request.upload(uploadCallbacks)

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
