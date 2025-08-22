import Foundation
import React
import UIKit
import SwiftUI
import Photos

@objc(RocateerImagePicker)
class RocateerImagePicker: NSObject {

  // MARK: - Properties
  private var resolve: RCTPromiseResolveBlock?
  private var reject: RCTPromiseRejectBlock?
  private var hostingController: UIViewController?

  // MARK: - Exported Methods
  @objc(open:resolver:rejecter:)
  func open(_ options: NSDictionary,
            resolver resolve: @escaping RCTPromiseResolveBlock,
            rejecter reject: @escaping RCTPromiseRejectBlock) {

    self.resolve = resolve
    self.reject = reject

    // Parse options
    let allowMultiple = (options["allowMultiple"] as? Bool) ?? false
    let maxSelection = options["maxSelection"] as? NSNumber // nil means unlimited when multiple
    let checkboxTintColor = options["checkboxTintColor"] as? String

    let requiredAccessLevel: PHAccessLevel = .readWrite
    let status = PHPhotoLibrary.authorizationStatus(for: requiredAccessLevel)

    switch status {
    case .authorized, .limited:
      self.presentImagePicker(allowMultiple: allowMultiple,
                              maxSelection: maxSelection,
                              checkboxTintColor: checkboxTintColor)

    case .notDetermined:
      PHPhotoLibrary.requestAuthorization(for: requiredAccessLevel) { newStatus in
        if newStatus == .authorized || newStatus == .limited {
          self.presentImagePicker(allowMultiple: allowMultiple,
                                  maxSelection: maxSelection,
                                  checkboxTintColor: checkboxTintColor)
        } else {
          self.reject?("PERMISSION_DENIED", "User denied photo library access.", nil)
        }
      }

    case .denied, .restricted:
      self.reject?("PERMISSION_DENIED", "User has previously denied photo library access.", nil)

    @unknown default:
      self.reject?("UNKNOWN_ERROR", "An unknown error occurred with photo library permissions.", nil)
    }
  }

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return true
  }

  // MARK: - Private Helper Methods
  private func presentImagePicker(allowMultiple: Bool,
                                  maxSelection: NSNumber?,
                                  checkboxTintColor: String?) {
    DispatchQueue.main.async {
      guard let rootVC = UIApplication.shared.keyWindow?.rootViewController else {
        self.reject?("NO_ROOT_VC", "Root view controller not found", nil)
        return
      }

      // Build the SwiftUI view, passing the options through.
      let imagePickerView = ImagePickerView(
        allowMultiple: allowMultiple,
        maxSelection: maxSelection?.intValue,
        checkboxTintColor: checkboxTintColor,
        onComplete: { selectedImageURLs in
          if let urls = selectedImageURLs {
            let urlStrings = urls.map { $0.absoluteString }
            self.resolve?(urlStrings)
          } else {
            self.resolve?([])
          }
          self.dismiss()
        },
        onDismiss: {
          // Align with JS wrapper expectation: cancel -> empty array
          self.resolve?([])
          self.dismiss()
        }
      )

      let hostingController = UIHostingController(rootView: imagePickerView)
      self.hostingController = hostingController

      rootVC.present(hostingController, animated: true)
    }
  }

  private func dismiss() {
    DispatchQueue.main.async {
        self.hostingController?.dismiss(animated: true, completion: nil)
    }
  }
}
