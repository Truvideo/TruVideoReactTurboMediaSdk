import { Text, View, StyleSheet } from 'react-native';
import { uploadMedia } from 'truvideo-react-turbo-media-sdk';

const result = uploadMedia("filepath", "tag", "metaData")
.then((res) => {
    console.log('Upload successful:', res);
})
.catch((err) => {
    console.log('Upload error:', err);
});

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
