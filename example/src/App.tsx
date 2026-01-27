// import { Text, View, StyleSheet } from 'react-native';
// import { MediaBuilder,type UploadProgressEvent } from '@trunpm/truvideo-react-turbo-media-sdk';

// const result = new MediaBuilder('filepath');
// result.setTag('key', 'value');
// result.setMetaData('key', 'value');
// var request = await result.build();

// const uploadCallbacks = {
//   onProgress: (event: UploadProgressEvent) => {
//     console.log(event);
//   },
//   onComplete: (event: any) => {
//     console.log(event);
//     // Use 'any' or proper type for parsed data
//   },
//   onError: (event: { id: string; error: any }) => {
//     console.log(event);
//   },
// };

// await request.upload(uploadCallbacks);

// export default function App() {
//   return (
//     <View style={styles.container}>
//       <Text>Result: </Text>
//     </View>
//   );
// }

// const styles = StyleSheet.create({
//   container: {
//     flex: 1,
//     alignItems: 'center',
//     justifyContent: 'center',
//   },
// });


import React, { useEffect, useState } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import { MediaBuilder, type UploadProgressEvent } from '@trunpm/truvideo-react-turbo-media-sdk';

export default function App() {
  const [request, setRequest] = useState<any>(null);

  useEffect(() => {
    const initializeMedia = async () => {
      const result = new MediaBuilder('filepath');
      result.setTag('key', 'value');
      result.setMetaData('key', 'value');
      const builtRequest = await result.build();
      setRequest(builtRequest);
    };

    initializeMedia();
  }, []);

  const uploadCallbacks = {
    onProgress: (event: UploadProgressEvent) => {
      console.log(event);
    },
    onComplete: (event: any) => {
      console.log(event);
    },
    onError: (event: { id: string; error: any }) => {
      console.log(event);
    },
  };

  return (
    <View style={styles.container}>
      <Text>Your App Content Here</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});