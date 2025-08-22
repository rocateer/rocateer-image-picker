//
//  ImagePickerView.swift
//  imagepicker
//
//  Created by rocket on 7/24/25.
//
import SwiftUI
import Photos

// MARK: - ImagePickerView
struct ImagePickerView: View {
  // MARK: - Properties
  var allowMultiple: Bool
  var maxSelection: Int?
  var checkboxTintColor: String?
  var onComplete: ([URL]?) -> Void
  var onDismiss: () -> Void

  @State private var assets: [PHAsset] = []
  @State private var selectedAssets: [PHAsset] = []
  @State private var showCamera = false

  private let columns: Int = 3
  private let spacing: CGFloat = 1

  // MARK: - Body
  var body: some View {
    NavigationView {
      GeometryReader { geometry in
        let cellWidth = (geometry.size.width - spacing * CGFloat(columns - 1)) / CGFloat(columns)
        let gridItems = Array(repeating: GridItem(.fixed(cellWidth), spacing: spacing), count: columns)

        ScrollView {
          LazyVGrid(columns: gridItems, spacing: spacing) {
            cameraButton(cellWidth: cellWidth)
            ForEach(assets, id: \.self) { asset in
              ZStack {
                PhotoThumbnailView(asset: asset, cellWidth: cellWidth)
                selectionOverlay(for: asset)
              }
              .frame(width: cellWidth, height: cellWidth)
              .contentShape(Rectangle())
              .onTapGesture {
                toggleSelection(for: asset)
              }
            }
          }
        }
        .navigationTitle("사진 선택")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: fetchPhotos)
        .toolbar { createToolbar() }
        .fullScreenCover(isPresented: $showCamera) {
          CameraView { capturedImageURL in
            showCamera = false
            if let url = capturedImageURL {
              onComplete([url])
            }
          }
        }
      }
    }
  }

  // MARK: - UI Components

  /// 카메라 버튼 UI (정사각형)
  private func cameraButton(cellWidth: CGFloat) -> some View {
    Button(action: { self.showCamera = true }) {
      ZStack {
        Color(uiColor: .systemGray6)
        Image(systemName: "camera.fill")
          .font(.title)
          .foregroundColor(.gray)
      }
      .frame(width: cellWidth, height: cellWidth)
      .clipped()
    }
  }

  /// 사진 선택 시 보여줄 오버레이 UI
  private func selectionOverlay(for asset: PHAsset) -> some View {
    ZStack(alignment: .topTrailing) {
      if selectedAssets.contains(asset) {
        Rectangle()
          .fill(Color.black.opacity(0.6))
        VStack {
          HStack {
            Spacer()
            // 시스템 아이콘 임시 사용 (assets에 ic_check 등록되어 있다면 Image("ic_check")로 교체)
            Image(systemName: "checkmark.circle.fill")
              .resizable()
              .frame(width: 24, height: 24)
              .foregroundColor(self.checkboxTint())
              .padding(4)
          }
          Spacer()
        }
      }
    }
  }

  /// 네비게이션 바 툴바 UI
  @ToolbarContentBuilder
  private func createToolbar() -> some ToolbarContent {
    // 뒤로가기 버튼
    ToolbarItem(placement: .navigationBarLeading) {
      Button(action: onDismiss) {
        Image(systemName: "chevron.backward").tint(.black)
      }
    }
    // 선택 완료 버튼
    ToolbarItem(placement: .navigationBarTrailing) {
      Button("선택 완료") {
        getURLs(for: selectedAssets) { urls in
          onComplete(urls)
        }
      }
      .disabled(selectedAssets.isEmpty)
    }
  }

  // MARK: - Logic Methods

  /// 사진 선택/해제 로직
  private func toggleSelection(for asset: PHAsset) {
    if let index = selectedAssets.firstIndex(of: asset) {
      selectedAssets.remove(at: index)
      return
    }

    // Not selected yet
    if !allowMultiple {
      // Single selection: replace selection
      selectedAssets = [asset]
      return
    }

    // Multiple selection
    if let limit = maxSelection, limit > 0 {
      if selectedAssets.count >= limit { return }
    }
    selectedAssets.append(asset)
  }

  /// 갤러리에서 사진을 가져오는 로직
  private func fetchPhotos() {
    let fetchOptions = PHFetchOptions()
    fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
    let fetchResult = PHAsset.fetchAssets(with: .image, options: fetchOptions)

    var fetchedAssets: [PHAsset] = []
    fetchResult.enumerateObjects { asset, _, _ in
      fetchedAssets.append(asset)
    }
    self.assets = fetchedAssets
  }

  /// 여러 개의 PHAsset을 URL 배열로 변환하는 로직
  private func getURLs(for assets: [PHAsset], completion: @escaping ([URL]) -> Void) {
    let group = DispatchGroup()
    var urls: [URL] = []

    for asset in assets {
      group.enter()
      asset.requestContentEditingInput(with: nil) { input, _ in
        if let url = input?.fullSizeImageURL {
          urls.append(url)
        }
        group.leave()
      }
    }

    group.notify(queue: .main) {
      completion(urls)
    }
  }

  // MARK: - Helpers
  private func checkboxTint() -> Color {
    if let hex = checkboxTintColor, let ui = UIColor(hexString: hex) {
      return Color(uiColor: ui)
    }
    return Color.green
  }
}

// MARK: - PhotoThumbnailView
struct PhotoThumbnailView: View {
  let asset: PHAsset
  let cellWidth: CGFloat
  @State private var image: Image?

  var body: some View {
    ZStack {
      if let image = image {
        image
          .resizable()
          .scaledToFill()
      } else {
        Color(uiColor: .systemGray6)
        ProgressView()
      }
    }
    .frame(width: cellWidth, height: cellWidth)
    .clipped()
    .onAppear(perform: loadImage)
  }

  private func loadImage() {
    let manager = PHImageManager.default()
    let size = CGSize(width: cellWidth * UIScreen.main.scale, height: cellWidth * UIScreen.main.scale)
    let options = PHImageRequestOptions()
    options.isSynchronous = false
    options.deliveryMode = .opportunistic

    manager.requestImage(for: asset, targetSize: size, contentMode: .aspectFill, options: options) { result, _ in
      if let result = result {
        self.image = Image(uiImage: result)
      }
    }
  }
}

extension UIColor {
  convenience init?(hexString: String) {
    var str = hexString.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    if str.hasPrefix("#") { str.removeFirst() }

    var alpha: UInt64 = 255
    var rgb: UInt64 = 0

    if str.count == 6 {
      guard Scanner(string: str).scanHexInt64(&rgb) else { return nil }
    } else if str.count == 8 {
      guard Scanner(string: str).scanHexInt64(&rgb) else { return nil }
      alpha = rgb & 0xFF
      rgb = rgb >> 8
    } else {
      return nil
    }

    let r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
    let g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
    let b = CGFloat(rgb & 0x0000FF) / 255.0
    let a = CGFloat(alpha) / 255.0

    self.init(red: r, green: g, blue: b, alpha: a)
  }
}
//#Preview {
//  ImagePickerView(title: "") {
//  }
//}
