import { Text, View, StyleSheet } from 'react-native';
import { MediaBuilder } from 'truvideo-react-turbo-media-sdk';

const result = new MediaBuilder('filepath');
result.setTag('key', 'value');
result.setMetaData('key', 'value');
var request = await result.build();

const uploadCallbacks = {
  onProgress: (event: { id: string; progress: string }) => {
    console.log(event);
  },
  onComplete: (event: any) => {
    console.log(event);
    // Use 'any' or proper type for parsed data
  },
  onError: (event: { id: string; error: any }) => {
    console.log(event);
  },
};

await request.upload(uploadCallbacks);

export default function App() {
  return (
    <View style={styles.container}>
      <Text>Result: </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
