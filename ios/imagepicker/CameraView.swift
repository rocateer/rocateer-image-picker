//
//  CameraView.swift
//  imagepicker
//
//  Created by rocket on 7/24/25.
//

import SwiftUI
import AVFoundation

struct CameraView: UIViewControllerRepresentable {
  var onComplete: (URL?) -> Void
  
  func makeUIViewController(context: Context) -> UIImagePickerController {
    let picker = UIImagePickerController()
    // 카메라 권한 확인 (선택 사항이지만 권장)
    if UIImagePickerController.isSourceTypeAvailable(.camera) {
      picker.sourceType = .camera
    } else {
      // 시뮬레이터 등에서 카메라를 사용할 수 없을 때
      // 이 부분은 에러 처리를 하거나, sourceType을 .photoLibrary로 변경할 수 있습니다.
      print("Camera not available")
    }
    picker.delegate = context.coordinator
    return picker
  }
  
  func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
  
  func makeCoordinator() -> Coordinator {
    Coordinator(self)
  }
  
  class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    var parent: CameraView
    
    init(_ parent: CameraView) {
      self.parent = parent
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
      guard let image = info[.originalImage] as? UIImage else {
        parent.onComplete(nil)
        return
      }
      
      let fileURL = saveImageToTempDirectory(image: image)
      parent.onComplete(fileURL)
    }
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
      parent.onComplete(nil)
    }
    
    private func saveImageToTempDirectory(image: UIImage) -> URL? {
      guard let data = image.jpegData(compressionQuality: 0.8) else { return nil }
      let tempDir = FileManager.default.temporaryDirectory
      let fileName = UUID().uuidString + ".jpg"
      let fileURL = tempDir.appendingPathComponent(fileName)
      
      do {
        try data.write(to: fileURL)
        return fileURL
      } catch {
        print("Error saving image: \(error)")
        return nil
      }
    }
  }
}
