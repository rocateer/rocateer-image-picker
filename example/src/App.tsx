import { useState } from 'react';
import { View, StyleSheet, Button, Image, FlatList, Text } from 'react-native';
import RocateerImagePicker from 'rocateer-image-picker';

export default function App() {
  const [images, setImages] = useState<string[]>([]);
  const [save, setSave] = useState(0);
  const handleOpenPicker = async () => {
    try {
      const selectedImages = await RocateerImagePicker.openImagePicker('이미지 선택');
      setImages(selectedImages);
    } catch (e) {
      console.log(e);
    }
  };

  const renderItem = ({ item }: { item: string }) => (
    <Image source={{ uri: item }} style={styles.imageItem} />
  );

  const handleMultiply = () => {
    const result = RocateerImagePicker.multiply(1, 20);
    setSave(result);
  };

  return (
    <View style={styles.container}>
      <Button title="이미지 선택 열기" onPress={handleOpenPicker} />
      <Button title="곱하기" onPress={handleMultiply} />
      <Text>{save}</Text>
      <FlatList
        data={images}
        keyExtractor={(item) => item}
        renderItem={renderItem}
        horizontal
        style={styles.list}
        showsHorizontalScrollIndicator={false}
      />
    </View>
  );
}


const styles = StyleSheet.create({
  container: { flex: 1, paddingTop: 40, backgroundColor: '#fff' },
  list: { marginTop: 16, height: 120 },
  imageItem: { width: 100, height: 100, marginRight: 10, borderRadius: 8 },
});
