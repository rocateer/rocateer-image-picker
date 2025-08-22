# rocateer-image-picker

rocateer.image.picker

## Installation


```sh
npm install rocateer-image-picker
```


## Usage

```js
import ImagePicker from 'rocateer-image-picker';

// Open image picker for single image selection
const singleImage = await ImagePicker.openPicker({
  multiple: false,
  cropping: true,
});

// Open image picker for multiple image selection
const multipleImages = await ImagePicker.openPicker({
  multiple: true,
  maxFiles: 5,
  cropping: false,
});

// Open camera to capture a photo
const capturedImage = await ImagePicker.openCamera({
  cropping: true,
});
```

### Options

| Option     | Type    | Description                                          | Default  |
|------------|---------|------------------------------------------------------|----------|
| multiple   | boolean | Allow selection of multiple images                    | false    |
| cropping   | boolean | Enable cropping after selection or capture           | false    |
| maxFiles   | number  | Maximum number of images that can be selected         | 1        |
| mediaType  | string  | Type of media to select (`photo`, `video`, or `all`) | `photo`  |
| compressQuality | number | Compression quality between 0 and 1               | 1        |

### Return Value

The promise resolves with an object or an array of objects (if `multiple` is true) with the following properties:

- `path` (string): The local file path of the selected image.
- `width` (number): The width of the image.
- `height` (number): The height of the image.
- `mime` (string): The MIME type of the image.
- `size` (number): The file size in bytes.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
