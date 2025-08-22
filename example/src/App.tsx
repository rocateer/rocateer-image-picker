import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  Button,
  Image,
  FlatList,
  Text,
  ScrollView,
} from 'react-native';
import { pickImage, pickImages, type ImageAsset } from 'rocateer-image-picker';

export default function App() {
  const [images, setImages] = useState<ImageAsset[]>([]);
  const [log, setLog] = useState<string>('');

  const appendLog = (msg: string) => setLog((prev) => `${prev}\n${msg}`.trim());

  const handleSingle = async () => {
    try {
      const asset = await pickImage({ checkboxTintColor: '#6750A4' });
      if (asset) setImages([asset]);
      appendLog(`single -> ${asset ? asset.uri : 'cancelled'}`);
    } catch (e: any) {
      appendLog(`single error: ${e?.message ?? String(e)}`);
    }
  };

  const handleMultiNoLimit = async () => {
    try {
      const assets = await pickImages({
        selectionMode: 'multiple',
        checkboxTintColor: '#00C853',
      });
      setImages(assets);
      appendLog(`multi(no limit) -> ${assets.length} selected`);
    } catch (e: any) {
      appendLog(`multi(no limit) error: ${e?.message ?? String(e)}`);
    }
  };

  const handleMultiMax3 = async () => {
    try {
      const assets = await pickImages({
        selectionMode: 'multiple',
        maxSelection: 3,
        checkboxTintColor: '#FF5722',
      });
      setImages(assets);
      appendLog(`multi(max=3) -> ${assets.length} selected`);
    } catch (e: any) {
      appendLog(`multi(max=3) error: ${e?.message ?? String(e)}`);
    }
  };

  const renderItem = ({ item }: { item: ImageAsset }) => (
    <Image source={{ uri: item.uri }} style={styles.imageItem} />
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Rocateer Image Picker — Example</Text>
      <View style={styles.row}>
        <Button title="단일 선택 (보라 체크)" onPress={handleSingle} />
      </View>
      <View style={styles.row}>
        <Button title="다중 선택 (제한 없음)" onPress={handleMultiNoLimit} />
      </View>
      <View style={styles.row}>
        <Button
          title="다중 선택 (최대 3장, 주황 체크)"
          onPress={handleMultiMax3}
        />
      </View>

      <Text style={styles.count}>선택: {images.length}장</Text>
      <FlatList
        data={images}
        keyExtractor={(it) => it.uri}
        renderItem={renderItem}
        horizontal
        style={styles.list}
        showsHorizontalScrollIndicator={false}
      />

      <Text style={styles.subtitle}>LOG</Text>
      <ScrollView style={styles.logBox}>
        <Text style={styles.logText}>{log || '(empty)'}</Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 48,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
  },
  title: { fontSize: 18, fontWeight: '600', marginBottom: 12 },
  subtitle: { marginTop: 16, fontSize: 14, fontWeight: '600' },
  row: { marginBottom: 10 },
  count: { marginTop: 12, marginBottom: 4 },
  list: { marginTop: 8, height: 120 },
  imageItem: {
    width: 100,
    height: 100,
    marginRight: 10,
    borderRadius: 8,
    backgroundColor: '#eee',
  },
  logBox: {
    marginTop: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 8,
    maxHeight: 160,
  },
  logText: { fontSize: 12, color: '#333' },
});
